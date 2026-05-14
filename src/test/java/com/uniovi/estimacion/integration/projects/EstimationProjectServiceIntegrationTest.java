package com.uniovi.estimacion.integration.projects;

import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.users.User;
import com.uniovi.estimacion.entities.users.UserRole;
import com.uniovi.estimacion.integration.AbstractIntegrationTest;
import com.uniovi.estimacion.repositories.projects.EstimationProjectRepository;
import com.uniovi.estimacion.repositories.users.UserRepository;
import com.uniovi.estimacion.services.projects.EstimationProjectService;
import com.uniovi.estimacion.services.projects.ProjectMembershipService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Estimation project service — integration tests")
class EstimationProjectServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EstimationProjectService estimationProjectService;

    @Autowired
    private ProjectMembershipService projectMembershipService;

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
    // create
    // =========================================================

    @Nested
    @DisplayName("Crear proyecto")
    class CreateProject {

        @Test
        @DisplayName("project manager crea proyecto y queda registrado como owner")
        void projectManagerCreatesProjectAndBecomesOwner() {
            // given
            authenticateAs(pmAlpha);
            EstimationProject project = new EstimationProject("Proyecto Nuevo Alpha", "Una descripción");

            // when
            EstimationProject created = estimationProjectService.create(project);

            // then
            assertThat(created.getId()).isNotNull();
            assertThat(created.getOwner()).isNotNull();
            assertThat(created.getOwner().getId()).isEqualTo(pmAlpha.getId());
        }

        @Test
        @DisplayName("admin crea proyecto y queda registrado como owner")
        void adminCreatesProjectAndBecomesOwner() {
            // given
            authenticateAs(admin);
            EstimationProject project = new EstimationProject("Proyecto del Admin", null);

            // when
            EstimationProject created = estimationProjectService.create(project);

            // then
            assertThat(created.getId()).isNotNull();
            assertThat(created.getOwner()).isNotNull();
            assertThat(created.getOwner().getId()).isEqualTo(admin.getId());
        }

        @Test
        @DisplayName("worker no puede crear proyecto — lanza IllegalStateException")
        void workerCannotCreateProject() {
            // given
            authenticateAs(workerAlpha);
            EstimationProject project = new EstimationProject("Proyecto por Worker", null);

            // when / then
            assertThatThrownBy(() -> estimationProjectService.create(project))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("proyecto persiste name, description, hourlyRate y currencyCode correctamente")
        void projectPersistsAllCoreFields() {
            // given
            authenticateAs(pmAlpha);
            EstimationProject project = new EstimationProject("Proyecto con Datos Completos", "Descripción larga");
            project.setHourlyRate(BigDecimal.valueOf(75.50));
            project.setCurrencyCode("USD");

            // when
            EstimationProject created = estimationProjectService.create(project);

            // then — recargar desde repositorio para comprobar persistencia real
            EstimationProject loaded = estimationProjectRepository.findById(created.getId()).orElseThrow();
            assertThat(loaded.getName()).isEqualTo("Proyecto con Datos Completos");
            assertThat(loaded.getDescription()).isEqualTo("Descripción larga");
            assertThat(loaded.getHourlyRate()).isEqualByComparingTo(BigDecimal.valueOf(75.50));
            assertThat(loaded.getCurrencyCode()).isEqualTo("USD");
        }

        @Test
        @DisplayName("proyecto tiene createdAt asignado tras la creación")
        void projectHasCreatedAtAfterCreation() {
            // given
            authenticateAs(pmAlpha);
            EstimationProject project = new EstimationProject("Proyecto Timestamps", null);

            // when
            EstimationProject created = estimationProjectService.create(project);

            // then
            assertThat(created.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("currencyCode se normaliza a mayúsculas al crear")
        void currencyCodeIsNormalizedToUppercase() {
            // given
            authenticateAs(pmAlpha);
            EstimationProject project = new EstimationProject("Proyecto Divisa Minúscula", null);
            project.setCurrencyCode("eur");

            // when
            EstimationProject created = estimationProjectService.create(project);

            // then
            assertThat(created.getCurrencyCode()).isEqualTo("EUR");
        }

        @Test
        @DisplayName("currencyCode se inicializa a EUR si no se especifica")
        void currencyCodeDefaultsToEurIfNotSet() {
            // given
            authenticateAs(pmAlpha);
            EstimationProject project = new EstimationProject("Proyecto Sin Divisa", null);
            project.setCurrencyCode(null);

            // when
            EstimationProject created = estimationProjectService.create(project);

            // then
            assertThat(created.getCurrencyCode()).isEqualTo("EUR");
        }
    }

    // =========================================================
    // findPageForCurrentUser
    // =========================================================

    @Nested
    @DisplayName("Listar proyectos según rol del usuario")
    class FindPageForCurrentUser {

        @Test
        @DisplayName("admin ve todos los proyectos del sistema")
        void adminSeesAllProjects() {
            // given
            EstimationProject projectA = saveProject("Proyecto Alpha", pmAlpha);
            EstimationProject projectB = saveProject("Proyecto Beta", pmBeta);
            authenticateAs(admin);

            // when
            Page<EstimationProject> page = estimationProjectService.findPageForCurrentUser(PageRequest.of(0, 100));

            // then
            List<Long> ids = page.getContent().stream().map(EstimationProject::getId).toList();
            assertThat(ids).contains(projectA.getId(), projectB.getId());
        }

        @Test
        @DisplayName("project manager ve solo sus propios proyectos")
        void projectManagerSeesOnlyOwnProjects() {
            // given
            EstimationProject projectA = saveProject("Proyecto Alpha", pmAlpha);
            EstimationProject projectB = saveProject("Proyecto Beta", pmBeta);
            authenticateAs(pmAlpha);

            // when
            Page<EstimationProject> page = estimationProjectService.findPageForCurrentUser(PageRequest.of(0, 100));

            // then
            List<Long> ids = page.getContent().stream().map(EstimationProject::getId).toList();
            assertThat(ids).contains(projectA.getId());
            assertThat(ids).doesNotContain(projectB.getId());
        }

        @Test
        @DisplayName("worker ve solo los proyectos a los que está asignado")
        void workerSeesOnlyAssignedProjects() {
            // given
            EstimationProject projectA = saveProject("Proyecto Alpha", pmAlpha);
            EstimationProject projectB = saveProject("Proyecto Beta", pmBeta);
            projectMembershipService.assignOrUpdateWorker(projectA.getId(), workerAlpha.getId(), false, false);
            authenticateAs(workerAlpha);

            // when
            Page<EstimationProject> page = estimationProjectService.findPageForCurrentUser(PageRequest.of(0, 100));

            // then
            List<Long> ids = page.getContent().stream().map(EstimationProject::getId).toList();
            assertThat(ids).contains(projectA.getId());
            assertThat(ids).doesNotContain(projectB.getId());
        }

        @Test
        @DisplayName("worker no asignado a ningún proyecto ve lista vacía")
        void unassignedWorkerSeesEmptyProjectList() {
            // given — workerAlpha no tiene membresías
            saveProject("Proyecto Alpha", pmAlpha);
            authenticateAs(workerAlpha);

            // when
            Page<EstimationProject> page = estimationProjectService.findPageForCurrentUser(PageRequest.of(0, 100));

            // then
            assertThat(page.isEmpty()).isTrue();
        }
    }

    // =========================================================
    // findAccessibleByIdForCurrentUser
    // =========================================================

    @Nested
    @DisplayName("Acceder al detalle de un proyecto")
    class FindAccessibleByIdForCurrentUser {

        @Test
        @DisplayName("admin puede acceder al detalle de cualquier proyecto")
        void adminCanAccessAnyProject() {
            // given
            EstimationProject projectA = saveProject("Proyecto Alpha", pmAlpha);
            EstimationProject projectB = saveProject("Proyecto Beta", pmBeta);
            authenticateAs(admin);

            // when / then
            assertThat(estimationProjectService.findAccessibleByIdForCurrentUser(projectA.getId())).isPresent();
            assertThat(estimationProjectService.findAccessibleByIdForCurrentUser(projectB.getId())).isPresent();
        }

        @Test
        @DisplayName("owner puede acceder al detalle de su propio proyecto")
        void ownerCanAccessOwnProject() {
            // given
            EstimationProject projectA = saveProject("Proyecto Alpha", pmAlpha);
            authenticateAs(pmAlpha);

            // when
            Optional<EstimationProject> result = estimationProjectService.findAccessibleByIdForCurrentUser(projectA.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(projectA.getId());
        }

        @Test
        @DisplayName("project manager no puede acceder al proyecto de otro manager")
        void projectManagerCannotAccessOtherManagerProject() {
            // given
            EstimationProject projectB = saveProject("Proyecto Beta", pmBeta);
            authenticateAs(pmAlpha);

            // when
            Optional<EstimationProject> result = estimationProjectService.findAccessibleByIdForCurrentUser(projectB.getId());

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("worker asignado puede acceder al proyecto")
        void assignedWorkerCanAccessProject() {
            // given
            EstimationProject projectA = saveProject("Proyecto Alpha", pmAlpha);
            projectMembershipService.assignOrUpdateWorker(projectA.getId(), workerAlpha.getId(), false, false);
            authenticateAs(workerAlpha);

            // when
            Optional<EstimationProject> result = estimationProjectService.findAccessibleByIdForCurrentUser(projectA.getId());

            // then
            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("worker no asignado no puede acceder al proyecto")
        void unassignedWorkerCannotAccessProject() {
            // given — workerAlpha sin membresía en projectA
            EstimationProject projectA = saveProject("Proyecto Alpha", pmAlpha);
            authenticateAs(workerAlpha);

            // when
            Optional<EstimationProject> result = estimationProjectService.findAccessibleByIdForCurrentUser(projectA.getId());

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("detalle de proyecto carga el owner correctamente")
        void projectDetailIncludesOwner() {
            // given
            EstimationProject projectA = saveProject("Proyecto Alpha", pmAlpha);
            authenticateAs(pmAlpha);

            // when
            Optional<EstimationProject> result = estimationProjectService.findAccessibleByIdForCurrentUser(projectA.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getOwner()).isNotNull();
            assertThat(result.get().getOwner().getId()).isEqualTo(pmAlpha.getId());
        }
    }

    // =========================================================
    // findManageableByIdForCurrentUser
    // =========================================================

    @Nested
    @DisplayName("Obtener proyecto para gestión")
    class FindManageableByIdForCurrentUser {

        @Test
        @DisplayName("admin puede gestionar cualquier proyecto")
        void adminCanManageAnyProject() {
            // given
            EstimationProject projectA = saveProject("Proyecto Alpha", pmAlpha);
            authenticateAs(admin);

            // when
            Optional<EstimationProject> result = estimationProjectService.findManageableByIdForCurrentUser(projectA.getId());

            // then
            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("owner puede gestionar su propio proyecto")
        void ownerCanManageOwnProject() {
            // given
            EstimationProject projectA = saveProject("Proyecto Alpha", pmAlpha);
            authenticateAs(pmAlpha);

            // when
            Optional<EstimationProject> result = estimationProjectService.findManageableByIdForCurrentUser(projectA.getId());

            // then
            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("project manager no puede gestionar proyecto de otro manager")
        void projectManagerCannotManageOtherProject() {
            // given
            EstimationProject projectB = saveProject("Proyecto Beta", pmBeta);
            authenticateAs(pmAlpha);

            // when
            Optional<EstimationProject> result = estimationProjectService.findManageableByIdForCurrentUser(projectB.getId());

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("worker no puede gestionar ningún proyecto aunque esté asignado con permisos")
        void workerCannotManageAnyProject() {
            // given
            EstimationProject projectA = saveProject("Proyecto Alpha", pmAlpha);
            projectMembershipService.assignOrUpdateWorker(projectA.getId(), workerAlpha.getId(), true, true);
            authenticateAs(workerAlpha);

            // when
            Optional<EstimationProject> result = estimationProjectService.findManageableByIdForCurrentUser(projectA.getId());

            // then
            assertThat(result).isEmpty();
        }
    }

    // =========================================================
    // updateBasicDataForCurrentUser
    // =========================================================

    @Nested
    @DisplayName("Actualizar datos básicos del proyecto")
    class UpdateBasicData {

        @Test
        @DisplayName("owner puede actualizar nombre y descripción de su proyecto")
        void ownerCanUpdateOwnProjectData() {
            // given
            EstimationProject projectA = saveProject("Nombre Original", pmAlpha);
            authenticateAs(pmAlpha);

            EstimationProject formData = new EstimationProject("Nombre Actualizado", "Nueva descripción");
            formData.setHourlyRate(BigDecimal.valueOf(90.00));
            formData.setCurrencyCode("GBP");

            // when
            boolean updated = estimationProjectService.updateBasicDataForCurrentUser(projectA.getId(), formData);

            // then
            assertThat(updated).isTrue();
            EstimationProject reloaded = estimationProjectRepository.findById(projectA.getId()).orElseThrow();
            assertThat(reloaded.getName()).isEqualTo("Nombre Actualizado");
            assertThat(reloaded.getDescription()).isEqualTo("Nueva descripción");
            assertThat(reloaded.getCurrencyCode()).isEqualTo("GBP");
        }

        @Test
        @DisplayName("project manager no puede actualizar proyecto de otro manager")
        void projectManagerCannotUpdateOtherManagerProject() {
            // given
            EstimationProject projectB = saveProject("Proyecto Beta", pmBeta);
            authenticateAs(pmAlpha);

            EstimationProject formData = new EstimationProject("Intento de Modificación", null);

            // when
            boolean updated = estimationProjectService.updateBasicDataForCurrentUser(projectB.getId(), formData);

            // then
            assertThat(updated).isFalse();
        }
    }

    // =========================================================
    // deleteAccessibleByIdForCurrentUser
    // =========================================================

    @Nested
    @DisplayName("Borrar proyecto")
    class DeleteProject {

        @Test
        @DisplayName("owner puede borrar su propio proyecto")
        void ownerCanDeleteOwnProject() {
            // given
            EstimationProject projectA = saveProject("Proyecto a Borrar", pmAlpha);
            authenticateAs(pmAlpha);

            // when
            boolean deleted = estimationProjectService.deleteAccessibleByIdForCurrentUser(projectA.getId());

            // then
            assertThat(deleted).isTrue();
            assertThat(estimationProjectRepository.findById(projectA.getId())).isEmpty();
        }

        @Test
        @DisplayName("project manager no puede borrar proyecto de otro manager")
        void projectManagerCannotDeleteOtherManagerProject() {
            // given
            EstimationProject projectB = saveProject("Proyecto Beta", pmBeta);
            authenticateAs(pmAlpha);

            // when
            boolean deleted = estimationProjectService.deleteAccessibleByIdForCurrentUser(projectB.getId());

            // then
            assertThat(deleted).isFalse();
            assertThat(estimationProjectRepository.findById(projectB.getId())).isPresent();
        }

        @Test
        @DisplayName("worker asignado con todos los permisos no puede borrar el proyecto")
        void workerWithFullPermissionsCannotDeleteProject() {
            // given
            EstimationProject projectA = saveProject("Proyecto Alpha", pmAlpha);
            projectMembershipService.assignOrUpdateWorker(projectA.getId(), workerAlpha.getId(), true, true);
            authenticateAs(workerAlpha);

            // when
            boolean deleted = estimationProjectService.deleteAccessibleByIdForCurrentUser(projectA.getId());

            // then
            assertThat(deleted).isFalse();
            assertThat(estimationProjectRepository.findById(projectA.getId())).isPresent();
        }

        @Test
        @DisplayName("admin puede borrar cualquier proyecto")
        void adminCanDeleteAnyProject() {
            // given
            EstimationProject projectA = saveProject("Proyecto a Borrar por Admin", pmAlpha);
            authenticateAs(admin);

            // when
            boolean deleted = estimationProjectService.deleteAccessibleByIdForCurrentUser(projectA.getId());

            // then
            assertThat(deleted).isTrue();
            assertThat(estimationProjectRepository.findById(projectA.getId())).isEmpty();
        }

        @Test
        @DisplayName("owner puede borrar proyecto con trabajadores asignados")
        void ownerCanDeleteProjectWithAssignedWorkers() {
            // given
            EstimationProject project = saveProject("Proyecto con trabajador", pmAlpha);
            projectMembershipService.assignOrUpdateWorker(project.getId(), workerAlpha.getId(), true, true);
            authenticateAs(pmAlpha);

            // when
            boolean deleted = estimationProjectService.deleteAccessibleByIdForCurrentUser(project.getId());

            // then
            assertThat(deleted).isTrue();
            assertThat(estimationProjectRepository.findById(project.getId())).isEmpty();
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

    /**
     * Guarda un proyecto directamente en el repositorio (para preparar datos de prueba).
     * El comportamiento bajo prueba siempre se ejecuta a través del service.
     */
    private EstimationProject saveProject(String name, User owner) {
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
