package com.uniovi.estimacion.integration.users;

import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.users.User;
import com.uniovi.estimacion.entities.users.UserRole;
import com.uniovi.estimacion.integration.AbstractIntegrationTest;
import com.uniovi.estimacion.repositories.projects.EstimationProjectRepository;
import com.uniovi.estimacion.repositories.users.UserRepository;
import com.uniovi.estimacion.services.users.UserManagementService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("User management service — integration tests")
class UserManagementServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserManagementService userManagementService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EstimationProjectRepository estimationProjectRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User admin;
    private User pmAlpha;
    private User pmBeta;
    private User workerAlpha;

    @BeforeEach
    void setUp() {
        admin = createUser("admin_test", "admin_test@test.com", UserRole.ROLE_ADMIN, null);
        pmAlpha = createUser("pm_alpha_test", "pm_alpha_test@test.com", UserRole.ROLE_PROJECT_MANAGER, null);
        pmBeta = createUser("pm_beta_test", "pm_beta_test@test.com", UserRole.ROLE_PROJECT_MANAGER, null);
        workerAlpha = createUser("worker_alpha_test", "worker_alpha_test@test.com", UserRole.ROLE_PROJECT_WORKER, pmAlpha);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // =========================================================
    // ADMIN — visibilidad de usuarios
    // =========================================================

    @Nested
    @DisplayName("Admin — visibilidad de usuarios")
    class AdminUserVisibility {

        @Test
        @DisplayName("admin ve todos los usuarios del sistema")
        void adminSeesAllUsers() {
            // given
            authenticateAs(admin);

            // when
            Page<User> page = userManagementService.findPageVisibleForCurrentUser(PageRequest.of(0, 100));

            // then — debe incluir al menos los 4 usuarios creados en setUp
            List<String> usernames = page.getContent().stream().map(User::getUsername).toList();
            assertThat(usernames).contains(
                    admin.getUsername(),
                    pmAlpha.getUsername(),
                    pmBeta.getUsername(),
                    workerAlpha.getUsername()
            );
        }

        @Test
        @DisplayName("admin puede recuperar cualquier usuario por id para gestionarlo")
        void adminCanFindAnyUserAsManageable() {
            // given
            authenticateAs(admin);

            // when
            Optional<User> foundPm = userManagementService.findManageableUserByIdForCurrentUser(pmAlpha.getId());
            Optional<User> foundWorker = userManagementService.findManageableUserByIdForCurrentUser(workerAlpha.getId());

            // then
            assertThat(foundPm).isPresent();
            assertThat(foundWorker).isPresent();
        }
    }

    // =========================================================
    // ADMIN — creación de usuarios
    // =========================================================

    @Nested
    @DisplayName("Admin — creación de usuarios")
    class AdminCreateUsers {

        @Test
        @DisplayName("admin puede crear un project manager")
        void adminCanCreateProjectManager() {
            // given
            authenticateAs(admin);

            // when
            Optional<User> result = userManagementService.createUserForCurrentUser(
                    "new_pm_test", "new_pm_test@test.com", "password",
                    UserRole.ROLE_PROJECT_MANAGER, null
            );

            // then
            assertThat(result).isPresent();
            User created = result.get();
            assertThat(created.getRole()).isEqualTo(UserRole.ROLE_PROJECT_MANAGER);
            assertThat(created.getProjectManager()).isNull();
            assertThat(created.getUsername()).isEqualTo("new_pm_test");
        }

        @Test
        @DisplayName("admin puede crear worker asociado a un project manager concreto")
        void adminCanCreateWorkerAssociatedToProjectManager() {
            // given
            authenticateAs(admin);

            // when
            Optional<User> result = userManagementService.createUserForCurrentUser(
                    "new_worker_test", "new_worker_test@test.com", "password",
                    UserRole.ROLE_PROJECT_WORKER, pmAlpha.getId()
            );

            // then
            assertThat(result).isPresent();
            User created = result.get();
            assertThat(created.getRole()).isEqualTo(UserRole.ROLE_PROJECT_WORKER);
            assertThat(created.getProjectManager()).isNotNull();
            assertThat(created.getProjectManager().getId()).isEqualTo(pmAlpha.getId());
        }

        @Test
        @DisplayName("admin no puede crear admin desde el flujo normal de gestión (se normaliza a PM)")
        void adminCannotCreateAdminThroughNormalFlow() {
            // given
            authenticateAs(admin);

            // when — ROLE_ADMIN no está en los roles creables, normalizeAdminCreatableRole lo convierte a PM
            Optional<User> result = userManagementService.createUserForCurrentUser(
                    "new_admin_test", "new_admin_test@test.com", "password",
                    UserRole.ROLE_ADMIN, null
            );

            // then — si se crea, el rol no puede ser ROLE_ADMIN
            if (result.isPresent()) {
                assertThat(result.get().getRole()).isNotEqualTo(UserRole.ROLE_ADMIN);
            }
            // si devuelve vacío también es correcto (no se creó ningún admin)
        }

        @Test
        @DisplayName("admin no puede crear worker sin project manager válido")
        void adminCannotCreateWorkerWithoutValidProjectManager() {
            // given
            authenticateAs(admin);

            // when — se pide crear worker pero projectManagerId es null
            Optional<User> result = userManagementService.createUserForCurrentUser(
                    "worker_no_pm_test", "worker_no_pm_test@test.com", "password",
                    UserRole.ROLE_PROJECT_WORKER, null
            );

            // then — el service rechaza la creación porque no hay PM válido
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("admin puede ver los roles disponibles para crear: PM y WORKER")
        void adminCanSeeCreatableRoles() {
            // given
            authenticateAs(admin);

            // when
            List<UserRole> roles = userManagementService.findCreatableRolesForCurrentUser();

            // then
            assertThat(roles).containsExactlyInAnyOrder(
                    UserRole.ROLE_PROJECT_MANAGER,
                    UserRole.ROLE_PROJECT_WORKER
            );
        }
    }

    // =========================================================
    // ADMIN — borrado de usuarios
    // =========================================================

    @Nested
    @DisplayName("Admin — borrado de usuarios")
    class AdminDeleteUsers {

        @Test
        @DisplayName("admin puede borrar un worker sin proyectos ni membresías")
        void adminCanDeleteWorkerWithNoProjects() {
            // given
            User workerToDelete = createUser("worker_del_test", "worker_del_test@test.com",
                    UserRole.ROLE_PROJECT_WORKER, pmAlpha);
            authenticateAs(admin);

            // when
            boolean deleted = userManagementService.deleteUserForCurrentUser(workerToDelete.getId());

            // then
            assertThat(deleted).isTrue();
            assertThat(userRepository.findById(workerToDelete.getId())).isEmpty();
        }

        @Test
        @DisplayName("admin no puede borrar a otro usuario con rol ROLE_ADMIN")
        void adminCannotDeleteAnotherAdmin() {
            // given
            User anotherAdmin = createUser("admin2_test", "admin2_test@test.com",
                    UserRole.ROLE_ADMIN, null);
            authenticateAs(admin);

            // when
            boolean deleted = userManagementService.deleteUserForCurrentUser(anotherAdmin.getId());

            // then
            assertThat(deleted).isFalse();
            assertThat(userRepository.findById(anotherAdmin.getId())).isPresent();
        }

        @Test
        @DisplayName("admin no puede borrarse a sí mismo")
        void adminCannotDeleteSelf() {
            // given
            authenticateAs(admin);

            // when
            boolean deleted = userManagementService.deleteUserForCurrentUser(admin.getId());

            // then
            assertThat(deleted).isFalse();
            assertThat(userRepository.findById(admin.getId())).isPresent();
        }

        @Test
        @DisplayName("admin no puede borrar project manager que tiene proyectos asociados")
        void adminCannotDeleteProjectManagerWithProjects() {
            // given
            createProject("Proyecto de pmAlpha", pmAlpha);
            authenticateAs(admin);

            // when
            boolean deleted = userManagementService.deleteUserForCurrentUser(pmAlpha.getId());

            // then
            assertThat(deleted).isFalse();
            assertThat(userRepository.findById(pmAlpha.getId())).isPresent();
        }

        @Test
        @DisplayName("admin no puede borrar project manager que tiene workers asociados")
        void adminCannotDeleteProjectManagerWithWorkers() {
            // given — pmAlpha ya tiene workerAlpha desde setUp
            authenticateAs(admin);

            // when
            boolean deleted = userManagementService.deleteUserForCurrentUser(pmAlpha.getId());

            // then
            assertThat(deleted).isFalse();
            assertThat(userRepository.findById(pmAlpha.getId())).isPresent();
        }

        @Test
        @DisplayName("admin puede borrar project manager que no tiene proyectos ni workers")
        void adminCanDeleteProjectManagerWithNoProjectsOrWorkers() {
            // given — pmBeta no tiene ni proyectos ni workers en setUp
            authenticateAs(admin);

            // when
            boolean deleted = userManagementService.deleteUserForCurrentUser(pmBeta.getId());

            // then
            assertThat(deleted).isTrue();
            assertThat(userRepository.findById(pmBeta.getId())).isEmpty();
        }
    }

    // =========================================================
    // PROJECT_MANAGER — visibilidad de usuarios
    // =========================================================

    @Nested
    @DisplayName("Project manager — visibilidad de usuarios")
    class ProjectManagerUserVisibility {

        @Test
        @DisplayName("project manager solo ve sus propios workers")
        void projectManagerSeesOnlyOwnWorkers() {
            // given
            authenticateAs(pmAlpha);

            // when
            Page<User> page = userManagementService.findPageVisibleForCurrentUser(PageRequest.of(0, 100));

            // then — solo workerAlpha está asociado a pmAlpha
            List<String> usernames = page.getContent().stream().map(User::getUsername).toList();
            assertThat(usernames).containsExactly(workerAlpha.getUsername());
        }

        @Test
        @DisplayName("project manager no ve los workers de otro project manager")
        void projectManagerDoesNotSeeOtherManagersWorkers() {
            // given
            User workerBeta = createUser("worker_beta_test", "worker_beta_test@test.com",
                    UserRole.ROLE_PROJECT_WORKER, pmBeta);
            authenticateAs(pmAlpha);

            // when
            Page<User> page = userManagementService.findPageVisibleForCurrentUser(PageRequest.of(0, 100));

            // then
            List<String> usernames = page.getContent().stream().map(User::getUsername).toList();
            assertThat(usernames).doesNotContain(workerBeta.getUsername());
        }

        @Test
        @DisplayName("project manager no puede recuperar un worker de otro manager por id")
        void projectManagerCannotFindWorkerOfAnotherManagerById() {
            // given
            User workerBeta = createUser("worker_beta_test", "worker_beta_test@test.com",
                    UserRole.ROLE_PROJECT_WORKER, pmBeta);
            authenticateAs(pmAlpha);

            // when
            Optional<User> result = userManagementService.findManageableUserByIdForCurrentUser(workerBeta.getId());

            // then
            assertThat(result).isEmpty();
        }
    }

    // =========================================================
    // PROJECT_MANAGER — creación de usuarios
    // =========================================================

    @Nested
    @DisplayName("Project manager — creación de usuarios")
    class ProjectManagerCreateUsers {

        @Test
        @DisplayName("project manager puede crear un worker propio")
        void projectManagerCanCreateOwnWorker() {
            // given
            authenticateAs(pmAlpha);

            // when
            Optional<User> result = userManagementService.createUserForCurrentUser(
                    "worker_new_pm_test", "worker_new_pm_test@test.com", "password",
                    UserRole.ROLE_PROJECT_WORKER, null
            );

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getRole()).isEqualTo(UserRole.ROLE_PROJECT_WORKER);
        }

        @Test
        @DisplayName("worker creado por project manager queda asociado a ese manager")
        void workerCreatedByPmIsLinkedToThatPm() {
            // given
            authenticateAs(pmAlpha);

            // when
            Optional<User> result = userManagementService.createUserForCurrentUser(
                    "worker_assoc_test", "worker_assoc_test@test.com", "password",
                    UserRole.ROLE_PROJECT_WORKER, null
            );

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getProjectManager()).isNotNull();
            assertThat(result.get().getProjectManager().getId()).isEqualTo(pmAlpha.getId());
        }

        @Test
        @DisplayName("project manager solo tiene ROLE_PROJECT_WORKER disponible para crear")
        void projectManagerCanOnlyCreateWorkerRole() {
            // given
            authenticateAs(pmAlpha);

            // when
            List<UserRole> creatableRoles = userManagementService.findCreatableRolesForCurrentUser();

            // then
            assertThat(creatableRoles).containsExactly(UserRole.ROLE_PROJECT_WORKER);
            assertThat(creatableRoles).doesNotContain(UserRole.ROLE_PROJECT_MANAGER, UserRole.ROLE_ADMIN);
        }

        @Test
        @DisplayName("project manager siempre crea WORKER aunque solicite un rol diferente")
        void projectManagerAlwaysCreatesWorkerRegardlessOfRequestedRole() {
            // given
            authenticateAs(pmAlpha);

            // when — se solicita crear un PM pero el service lo fuerza a WORKER
            Optional<User> result = userManagementService.createUserForCurrentUser(
                    "forced_worker_test", "forced_worker_test@test.com", "password",
                    UserRole.ROLE_PROJECT_MANAGER, null
            );

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getRole()).isEqualTo(UserRole.ROLE_PROJECT_WORKER);
            assertThat(result.get().getProjectManager().getId()).isEqualTo(pmAlpha.getId());
        }
    }

    // =========================================================
    // PROJECT_MANAGER — restricciones de acceso entre managers
    // =========================================================

    @Nested
    @DisplayName("Project manager — restricciones de acceso a otros managers")
    class ProjectManagerCrossAccessRestrictions {

        @Test
        @DisplayName("project manager no puede borrar worker de otro manager")
        void projectManagerCannotDeleteWorkerOfAnotherManager() {
            // given
            User workerBeta = createUser("worker_beta_test", "worker_beta_test@test.com",
                    UserRole.ROLE_PROJECT_WORKER, pmBeta);
            authenticateAs(pmAlpha);

            // when
            boolean deleted = userManagementService.deleteUserForCurrentUser(workerBeta.getId());

            // then
            assertThat(deleted).isFalse();
            assertThat(userRepository.findById(workerBeta.getId())).isPresent();
        }

        @Test
        @DisplayName("project manager puede borrar su propio worker")
        void projectManagerCanDeleteOwnWorker() {
            // given
            User workerToDelete = createUser("worker_del_pm_test", "worker_del_pm_test@test.com",
                    UserRole.ROLE_PROJECT_WORKER, pmAlpha);
            authenticateAs(pmAlpha);

            // when
            boolean deleted = userManagementService.deleteUserForCurrentUser(workerToDelete.getId());

            // then
            assertThat(deleted).isTrue();
            assertThat(userRepository.findById(workerToDelete.getId())).isEmpty();
        }

        @Test
        @DisplayName("project manager puede actualizar datos básicos de su propio worker")
        void projectManagerCanUpdateOwnWorkerBasicData() {
            // given
            authenticateAs(pmAlpha);

            // when
            boolean updated = userManagementService.updateUserForCurrentUser(
                    workerAlpha.getId(), "worker_renamed_test", "worker_renamed_test@test.com", null
            );

            // then
            assertThat(updated).isTrue();
            User reloaded = userRepository.findById(workerAlpha.getId()).orElseThrow();
            assertThat(reloaded.getUsername()).isEqualTo("worker_renamed_test");
        }
    }

    // =========================================================
    // PROJECT_WORKER — restricciones de acceso a gestión de usuarios
    // =========================================================

    @Nested
    @DisplayName("Project worker — restricciones de acceso a gestión de usuarios")
    class ProjectWorkerRestrictions {

        @Test
        @DisplayName("worker no tiene permiso para gestionar usuarios")
        void workerCannotManageUsers() {
            // given
            authenticateAs(workerAlpha);

            // when
            boolean canManage = userManagementService.canManageUsers();

            // then
            assertThat(canManage).isFalse();
        }

        @Test
        @DisplayName("worker no ve ningún usuario en la lista de gestión")
        void workerSeesNoUsersInManagementList() {
            // given
            authenticateAs(workerAlpha);

            // when
            Page<User> page = userManagementService.findPageVisibleForCurrentUser(PageRequest.of(0, 100));

            // then
            assertThat(page.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("worker no puede crear usuarios")
        void workerCannotCreateUsers() {
            // given
            authenticateAs(workerAlpha);

            // when
            Optional<User> result = userManagementService.createUserForCurrentUser(
                    "new_user_by_worker_test", "new_user_by_worker_test@test.com", "password",
                    UserRole.ROLE_PROJECT_WORKER, null
            );

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("worker no tiene roles disponibles para crear")
        void workerHasNoCreatableRoles() {
            // given
            authenticateAs(workerAlpha);

            // when
            List<UserRole> roles = userManagementService.findCreatableRolesForCurrentUser();

            // then
            assertThat(roles).isEmpty();
        }

        @Test
        @DisplayName("worker no puede recuperar ningún usuario por id para gestionarlo")
        void workerCannotFindAnyUserAsManageable() {
            // given
            authenticateAs(workerAlpha);

            // when
            Optional<User> result = userManagementService.findManageableUserByIdForCurrentUser(pmAlpha.getId());

            // then
            assertThat(result).isEmpty();
        }
    }

    // =========================================================
    // Helpers
    // =========================================================

    private User createUser(String username, String email, UserRole role, User projectManager) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("password"));
        user.setRole(role);
        user.setEnabled(true);
        user.setProjectManager(projectManager);
        return userRepository.save(user);
    }

    private EstimationProject createProject(String name, User owner) {
        EstimationProject project = new EstimationProject(name, null);
        project.setOwner(owner);
        return estimationProjectRepository.save(project);
    }

    private void authenticateAs(User user) {
        var auth = new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                "password",
                List.of(new SimpleGrantedAuthority(user.getRole().getAuthority()))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
