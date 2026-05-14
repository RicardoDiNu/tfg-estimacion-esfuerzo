package com.uniovi.estimacion.integration.security;

import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.projects.ProjectMembership;
import com.uniovi.estimacion.entities.users.User;
import com.uniovi.estimacion.entities.users.UserRole;
import com.uniovi.estimacion.integration.AbstractIntegrationTest;
import com.uniovi.estimacion.repositories.projects.EstimationProjectRepository;
import com.uniovi.estimacion.repositories.projects.ProjectMembershipRepository;
import com.uniovi.estimacion.repositories.users.UserRepository;
import com.uniovi.estimacion.services.projects.ProjectAuthorizationService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Project authorization service — integration tests")
class ProjectAuthorizationServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ProjectAuthorizationService projectAuthorizationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EstimationProjectRepository estimationProjectRepository;

    @Autowired
    private ProjectMembershipRepository projectMembershipRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User admin;
    private User pmAlpha;
    private User pmBeta;
    private User workerAlpha;
    private User workerBeta;
    private EstimationProject projectA;
    private EstimationProject projectB;

    @BeforeEach
    void setUp() {
        admin = createUser("admin_test", "admin_test@test.com", UserRole.ROLE_ADMIN, null);
        pmAlpha = createUser("pm_alpha_test", "pm_alpha_test@test.com", UserRole.ROLE_PROJECT_MANAGER, null);
        pmBeta = createUser("pm_beta_test", "pm_beta_test@test.com", UserRole.ROLE_PROJECT_MANAGER, null);
        workerAlpha = createUser("worker_alpha_test", "worker_alpha_test@test.com", UserRole.ROLE_PROJECT_WORKER, pmAlpha);
        workerBeta = createUser("worker_beta_test", "worker_beta_test@test.com", UserRole.ROLE_PROJECT_WORKER, pmBeta);

        projectA = createProject("Proyecto Alpha", pmAlpha);
        projectB = createProject("Proyecto Beta", pmBeta);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // =========================================================
    // canCreateProjects
    // =========================================================

    @Nested
    @DisplayName("canCreateProjects")
    class CanCreateProjects {

        @Test
        @DisplayName("admin puede crear proyectos")
        void adminCanCreateProjects() {
            // given
            authenticateAs(admin);

            // when
            boolean result = projectAuthorizationService.canCreateProjects();

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("project manager puede crear proyectos")
        void projectManagerCanCreateProjects() {
            // given
            authenticateAs(pmAlpha);

            // when
            boolean result = projectAuthorizationService.canCreateProjects();

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("project worker no puede crear proyectos")
        void projectWorkerCannotCreateProjects() {
            // given
            authenticateAs(workerAlpha);

            // when
            boolean result = projectAuthorizationService.canCreateProjects();

            // then
            assertThat(result).isFalse();
        }
    }

    // =========================================================
    // canAccessProject
    // =========================================================

    @Nested
    @DisplayName("canAccessProject")
    class CanAccessProject {

        @Test
        @DisplayName("admin puede acceder a cualquier proyecto existente")
        void adminCanAccessAnyProject() {
            // given
            authenticateAs(admin);

            // when / then
            assertThat(projectAuthorizationService.canAccessProject(projectA.getId())).isTrue();
            assertThat(projectAuthorizationService.canAccessProject(projectB.getId())).isTrue();
        }

        @Test
        @DisplayName("owner puede acceder a su propio proyecto")
        void ownerCanAccessOwnProject() {
            // given
            authenticateAs(pmAlpha);

            // when
            boolean result = projectAuthorizationService.canAccessProject(projectA.getId());

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("project manager no puede acceder al proyecto de otro manager")
        void projectManagerCannotAccessOtherManagerProject() {
            // given
            authenticateAs(pmAlpha);

            // when
            boolean result = projectAuthorizationService.canAccessProject(projectB.getId());

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("worker asignado al proyecto puede acceder")
        void assignedWorkerCanAccessProject() {
            // given
            assignWorker(projectA, workerAlpha, false, false);
            authenticateAs(workerAlpha);

            // when
            boolean result = projectAuthorizationService.canAccessProject(projectA.getId());

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("worker no asignado no puede acceder al proyecto")
        void unassignedWorkerCannotAccessProject() {
            // given — workerBeta no tiene membresía en projectA
            authenticateAs(workerBeta);

            // when
            boolean result = projectAuthorizationService.canAccessProject(projectA.getId());

            // then
            assertThat(result).isFalse();
        }
    }

    // =========================================================
    // canManageProject
    // =========================================================

    @Nested
    @DisplayName("canManageProject")
    class CanManageProject {

        @Test
        @DisplayName("admin puede gestionar cualquier proyecto existente")
        void adminCanManageAnyProject() {
            // given
            authenticateAs(admin);

            // when / then
            assertThat(projectAuthorizationService.canManageProject(projectA.getId())).isTrue();
            assertThat(projectAuthorizationService.canManageProject(projectB.getId())).isTrue();
        }

        @Test
        @DisplayName("owner puede gestionar su propio proyecto")
        void ownerCanManageOwnProject() {
            // given
            authenticateAs(pmAlpha);

            // when
            boolean result = projectAuthorizationService.canManageProject(projectA.getId());

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("project manager no puede gestionar proyecto de otro manager")
        void projectManagerCannotManageOtherManagerProject() {
            // given
            authenticateAs(pmAlpha);

            // when
            boolean result = projectAuthorizationService.canManageProject(projectB.getId());

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("worker asignado con todos los permisos no puede gestionar el proyecto")
        void assignedWorkerWithFullPermissionsCannotManageProject() {
            // given
            assignWorker(projectA, workerAlpha, true, true);
            authenticateAs(workerAlpha);

            // when
            boolean result = projectAuthorizationService.canManageProject(projectA.getId());

            // then
            assertThat(result).isFalse();
        }
    }

    // =========================================================
    // canEditEstimationData
    // =========================================================

    @Nested
    @DisplayName("canEditEstimationData")
    class CanEditEstimationData {

        @Test
        @DisplayName("admin puede editar datos de estimación de cualquier proyecto")
        void adminCanEditEstimationData() {
            // given
            authenticateAs(admin);

            // when
            boolean result = projectAuthorizationService.canEditEstimationData(projectA.getId());

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("owner puede editar datos de estimación de su proyecto")
        void ownerCanEditEstimationData() {
            // given
            authenticateAs(pmAlpha);

            // when
            boolean result = projectAuthorizationService.canEditEstimationData(projectA.getId());

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("worker con canEditEstimationData=true puede editar datos de estimación")
        void workerWithPermissionCanEditEstimationData() {
            // given
            assignWorker(projectA, workerAlpha, true, false);
            authenticateAs(workerAlpha);

            // when
            boolean result = projectAuthorizationService.canEditEstimationData(projectA.getId());

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("worker con canEditEstimationData=false no puede editar datos de estimación")
        void workerWithoutPermissionCannotEditEstimationData() {
            // given
            assignWorker(projectA, workerAlpha, false, false);
            authenticateAs(workerAlpha);

            // when
            boolean result = projectAuthorizationService.canEditEstimationData(projectA.getId());

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("worker con solo canManageEffortConversions no puede editar datos de estimación")
        void workerWithOnlyConversionsPermissionCannotEditEstimationData() {
            // given
            assignWorker(projectA, workerAlpha, false, true);
            authenticateAs(workerAlpha);

            // when
            boolean result = projectAuthorizationService.canEditEstimationData(projectA.getId());

            // then
            assertThat(result).isFalse();
        }
    }

    // =========================================================
    // canManageEffortConversions
    // =========================================================

    @Nested
    @DisplayName("canManageEffortConversions")
    class CanManageEffortConversions {

        @Test
        @DisplayName("admin puede gestionar conversiones de esfuerzo")
        void adminCanManageEffortConversions() {
            // given
            authenticateAs(admin);

            // when
            boolean result = projectAuthorizationService.canManageEffortConversions(projectA.getId());

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("owner puede gestionar conversiones de su proyecto")
        void ownerCanManageEffortConversions() {
            // given
            authenticateAs(pmAlpha);

            // when
            boolean result = projectAuthorizationService.canManageEffortConversions(projectA.getId());

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("worker con canManageEffortConversions=true puede gestionar conversiones")
        void workerWithPermissionCanManageEffortConversions() {
            // given
            assignWorker(projectA, workerAlpha, false, true);
            authenticateAs(workerAlpha);

            // when
            boolean result = projectAuthorizationService.canManageEffortConversions(projectA.getId());

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("worker con canManageEffortConversions=false no puede gestionar conversiones")
        void workerWithoutPermissionCannotManageEffortConversions() {
            // given
            assignWorker(projectA, workerAlpha, false, false);
            authenticateAs(workerAlpha);

            // when
            boolean result = projectAuthorizationService.canManageEffortConversions(projectA.getId());

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("worker con solo canEditEstimationData no puede gestionar conversiones de esfuerzo")
        void workerWithOnlyEditPermissionCannotManageConversions() {
            // given
            assignWorker(projectA, workerAlpha, true, false);
            authenticateAs(workerAlpha);

            // when
            boolean result = projectAuthorizationService.canManageEffortConversions(projectA.getId());

            // then
            assertThat(result).isFalse();
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

    private void assignWorker(EstimationProject project, User worker,
                              boolean canEditEstimationData, boolean canManageEffortConversions) {
        ProjectMembership membership = new ProjectMembership(
                project, worker, canEditEstimationData, canManageEffortConversions
        );
        projectMembershipRepository.save(membership);
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
