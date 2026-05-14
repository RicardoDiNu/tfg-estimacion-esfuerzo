package com.uniovi.estimacion.integration.security;

import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.projects.ProjectMembership;
import com.uniovi.estimacion.entities.users.User;
import com.uniovi.estimacion.entities.users.UserRole;
import com.uniovi.estimacion.integration.AbstractIntegrationTest;
import com.uniovi.estimacion.repositories.projects.EstimationProjectRepository;
import com.uniovi.estimacion.repositories.projects.ProjectMembershipRepository;
import com.uniovi.estimacion.repositories.users.UserRepository;
import com.uniovi.estimacion.services.projects.ProjectMembershipService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Project membership — integration tests")
class ProjectMembershipIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ProjectMembershipService projectMembershipService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EstimationProjectRepository estimationProjectRepository;

    @Autowired
    private ProjectMembershipRepository projectMembershipRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User pmAlpha;
    private User pmBeta;
    private User workerAlpha;
    private User workerBeta;
    private EstimationProject projectA;
    private EstimationProject projectB;

    @BeforeEach
    void setUp() {
        pmAlpha = createUser("pm_alpha_test", "pm_alpha_test@test.com", UserRole.ROLE_PROJECT_MANAGER, null);
        pmBeta = createUser("pm_beta_test", "pm_beta_test@test.com", UserRole.ROLE_PROJECT_MANAGER, null);
        workerAlpha = createUser("worker_alpha_test", "worker_alpha_test@test.com", UserRole.ROLE_PROJECT_WORKER, pmAlpha);
        workerBeta = createUser("worker_beta_test", "worker_beta_test@test.com", UserRole.ROLE_PROJECT_WORKER, pmBeta);

        projectA = createProject("Proyecto Alpha", pmAlpha);
        projectB = createProject("Proyecto Beta", pmBeta);
    }

    // =========================================================
    // assignOrUpdateWorker — asignación inicial
    // =========================================================

    @Nested
    @DisplayName("Asignar trabajador a proyecto")
    class AssignWorker {

        @Test
        @DisplayName("asignar worker crea membresía con los permisos indicados")
        void assignWorkerCreatesMembershipWithCorrectPermissions() {
            // given / when
            Optional<ProjectMembership> result = projectMembershipService.assignOrUpdateWorker(
                    projectA.getId(), workerAlpha.getId(), true, false
            );

            // then
            assertThat(result).isPresent();
            ProjectMembership membership = result.get();
            assertThat(membership.getWorker().getUsername()).isEqualTo(workerAlpha.getUsername());
            assertThat(membership.getProject().getId()).isEqualTo(projectA.getId());
            assertThat(membership.getCanEditEstimationData()).isTrue();
            assertThat(membership.getCanManageEffortConversions()).isFalse();
        }

        @Test
        @DisplayName("asignar worker con canManageEffortConversions=true persiste el permiso")
        void assignWorkerWithConversionsPermissionPersistsCorrectly() {
            // given / when
            Optional<ProjectMembership> result = projectMembershipService.assignOrUpdateWorker(
                    projectA.getId(), workerAlpha.getId(), false, true
            );

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getCanManageEffortConversions()).isTrue();
            assertThat(result.get().getCanEditEstimationData()).isFalse();
        }

        @Test
        @DisplayName("asignar a un project manager (no worker) devuelve vacío")
        void assignProjectManagerReturnEmpty() {
            // given — pmBeta tiene ROLE_PROJECT_MANAGER, no ROLE_PROJECT_WORKER

            // when
            Optional<ProjectMembership> result = projectMembershipService.assignOrUpdateWorker(
                    projectA.getId(), pmBeta.getId(), true, true
            );

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("asignar worker a proyecto inexistente devuelve vacío")
        void assignWorkerToNonExistentProjectReturnsEmpty() {
            // given
            long nonExistentProjectId = Long.MAX_VALUE;

            // when
            Optional<ProjectMembership> result = projectMembershipService.assignOrUpdateWorker(
                    nonExistentProjectId, workerAlpha.getId(), true, false
            );

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("asignar worker inexistente devuelve vacío")
        void assignNonExistentWorkerReturnsEmpty() {
            // given
            long nonExistentWorkerId = Long.MAX_VALUE;

            // when
            Optional<ProjectMembership> result = projectMembershipService.assignOrUpdateWorker(
                    projectA.getId(), nonExistentWorkerId, true, false
            );

            // then
            assertThat(result).isEmpty();
        }
    }

    // =========================================================
    // assignOrUpdateWorker — actualización de permisos
    // =========================================================

    @Nested
    @DisplayName("Actualizar permisos de membresía existente")
    class UpdateMembershipPermissions {

        @Test
        @DisplayName("llamar a assignOrUpdateWorker sobre membresía existente actualiza los permisos")
        void callingAssignOnExistingMembershipUpdatesPermissions() {
            // given — crear membresía inicial sin permisos
            projectMembershipService.assignOrUpdateWorker(projectA.getId(), workerAlpha.getId(), false, false);

            // when — actualizar concediendo todos los permisos
            Optional<ProjectMembership> result = projectMembershipService.assignOrUpdateWorker(
                    projectA.getId(), workerAlpha.getId(), true, true
            );

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getCanEditEstimationData()).isTrue();
            assertThat(result.get().getCanManageEffortConversions()).isTrue();
        }

        @Test
        @DisplayName("revocar permisos en membresía existente guarda los valores en false")
        void revokingPermissionsOnExistingMembershipPersistsFalse() {
            // given — crear con permisos completos
            projectMembershipService.assignOrUpdateWorker(projectA.getId(), workerAlpha.getId(), true, true);

            // when — revocar todos los permisos
            Optional<ProjectMembership> result = projectMembershipService.assignOrUpdateWorker(
                    projectA.getId(), workerAlpha.getId(), false, false
            );

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getCanEditEstimationData()).isFalse();
            assertThat(result.get().getCanManageEffortConversions()).isFalse();
        }

        @Test
        @DisplayName("no se duplica la membresía al actualizar: sigue existiendo solo una")
        void updatingMembershipDoesNotCreateDuplicate() {
            // given
            projectMembershipService.assignOrUpdateWorker(projectA.getId(), workerAlpha.getId(), false, false);
            projectMembershipService.assignOrUpdateWorker(projectA.getId(), workerAlpha.getId(), true, false);

            // when
            List<ProjectMembership> memberships = projectMembershipService.findByProjectId(projectA.getId());

            // then
            long countForWorker = memberships.stream()
                    .filter(m -> m.getWorker().getId().equals(workerAlpha.getId()))
                    .count();
            assertThat(countForWorker).isEqualTo(1);
        }
    }

    // =========================================================
    // removeWorkerFromProject
    // =========================================================

    @Nested
    @DisplayName("Eliminar trabajador de proyecto")
    class RemoveWorker {

        @Test
        @DisplayName("eliminar worker asignado devuelve true y borra la membresía")
        void removeAssignedWorkerReturnsTrueAndDeletesMembership() {
            // given
            projectMembershipService.assignOrUpdateWorker(projectA.getId(), workerAlpha.getId(), false, false);
            assertThat(projectMembershipRepository
                    .existsByProjectIdAndWorkerId(projectA.getId(), workerAlpha.getId())).isTrue();

            // when
            boolean removed = projectMembershipService.removeWorkerFromProject(
                    projectA.getId(), workerAlpha.getId()
            );

            // then
            assertThat(removed).isTrue();
            assertThat(projectMembershipRepository
                    .existsByProjectIdAndWorkerId(projectA.getId(), workerAlpha.getId())).isFalse();
        }

        @Test
        @DisplayName("eliminar worker no asignado devuelve false")
        void removeUnassignedWorkerReturnsFalse() {
            // given — workerBeta no tiene membresía en projectA

            // when
            boolean removed = projectMembershipService.removeWorkerFromProject(
                    projectA.getId(), workerBeta.getId()
            );

            // then
            assertThat(removed).isFalse();
        }
    }

    // =========================================================
    // isWorkerAssignedToProject
    // =========================================================

    @Nested
    @DisplayName("Consultar si worker está asignado")
    class IsWorkerAssigned {

        @Test
        @DisplayName("worker asignado aparece como asignado al proyecto")
        void assignedWorkerReportsAsAssigned() {
            // given
            projectMembershipService.assignOrUpdateWorker(projectA.getId(), workerAlpha.getId(), false, false);

            // when
            boolean assigned = projectMembershipService.isWorkerAssignedToProject(
                    projectA.getId(), workerAlpha.getUsername()
            );

            // then
            assertThat(assigned).isTrue();
        }

        @Test
        @DisplayName("worker no asignado aparece como no asignado al proyecto")
        void unassignedWorkerReportsAsNotAssigned() {
            // given — workerBeta no tiene membresía en projectA

            // when
            boolean assigned = projectMembershipService.isWorkerAssignedToProject(
                    projectA.getId(), workerBeta.getUsername()
            );

            // then
            assertThat(assigned).isFalse();
        }

        @Test
        @DisplayName("tras eliminar la membresía el worker deja de estar asignado")
        void afterRemovingMembershipWorkerIsNoLongerAssigned() {
            // given
            projectMembershipService.assignOrUpdateWorker(projectA.getId(), workerAlpha.getId(), false, false);
            projectMembershipService.removeWorkerFromProject(projectA.getId(), workerAlpha.getId());

            // when
            boolean assigned = projectMembershipService.isWorkerAssignedToProject(
                    projectA.getId(), workerAlpha.getUsername()
            );

            // then
            assertThat(assigned).isFalse();
        }
    }

    // =========================================================
    // canEditEstimationData / canManageEffortConversions
    // =========================================================

    @Nested
    @DisplayName("Permisos de membresía")
    class MembershipPermissions {

        @Test
        @DisplayName("canEditEstimationData=true refleja el permiso almacenado en la membresía")
        void canEditEstimationDataTrueReflectsPermission() {
            // given
            projectMembershipService.assignOrUpdateWorker(projectA.getId(), workerAlpha.getId(), true, false);

            // when
            boolean canEdit = projectMembershipService.canEditEstimationData(
                    projectA.getId(), workerAlpha.getUsername()
            );

            // then
            assertThat(canEdit).isTrue();
        }

        @Test
        @DisplayName("canEditEstimationData=false refleja el permiso almacenado en la membresía")
        void canEditEstimationDataFalseReflectsPermission() {
            // given — solo tiene canManageEffortConversions
            projectMembershipService.assignOrUpdateWorker(projectA.getId(), workerAlpha.getId(), false, true);

            // when
            boolean canEdit = projectMembershipService.canEditEstimationData(
                    projectA.getId(), workerAlpha.getUsername()
            );

            // then
            assertThat(canEdit).isFalse();
        }

        @Test
        @DisplayName("canManageEffortConversions=true refleja el permiso almacenado en la membresía")
        void canManageEffortConversionsTrueReflectsPermission() {
            // given
            projectMembershipService.assignOrUpdateWorker(projectA.getId(), workerAlpha.getId(), false, true);

            // when
            boolean canManage = projectMembershipService.canManageEffortConversions(
                    projectA.getId(), workerAlpha.getUsername()
            );

            // then
            assertThat(canManage).isTrue();
        }

        @Test
        @DisplayName("canManageEffortConversions=false refleja el permiso almacenado en la membresía")
        void canManageEffortConversionsFalseReflectsPermission() {
            // given — solo tiene canEditEstimationData
            projectMembershipService.assignOrUpdateWorker(projectA.getId(), workerAlpha.getId(), true, false);

            // when
            boolean canManage = projectMembershipService.canManageEffortConversions(
                    projectA.getId(), workerAlpha.getUsername()
            );

            // then
            assertThat(canManage).isFalse();
        }

        @Test
        @DisplayName("los permisos canEditEstimationData y canManageEffortConversions son independientes")
        void bothPermissionsAreIndependent() {
            // given — workerAlpha solo puede editar; workerBeta solo puede gestionar conversiones
            projectMembershipService.assignOrUpdateWorker(projectA.getId(), workerAlpha.getId(), true, false);
            projectMembershipService.assignOrUpdateWorker(projectB.getId(), workerBeta.getId(), false, true);

            // when
            boolean workerAlphaEdit = projectMembershipService.canEditEstimationData(projectA.getId(), workerAlpha.getUsername());
            boolean workerAlphaConv = projectMembershipService.canManageEffortConversions(projectA.getId(), workerAlpha.getUsername());
            boolean workerBetaEdit = projectMembershipService.canEditEstimationData(projectB.getId(), workerBeta.getUsername());
            boolean workerBetaConv = projectMembershipService.canManageEffortConversions(projectB.getId(), workerBeta.getUsername());

            // then
            assertThat(workerAlphaEdit).isTrue();
            assertThat(workerAlphaConv).isFalse();
            assertThat(workerBetaEdit).isFalse();
            assertThat(workerBetaConv).isTrue();
        }

        @Test
        @DisplayName("worker no asignado no puede editar datos de estimación")
        void unassignedWorkerCannotEditEstimationData() {
            // given — workerBeta no tiene membresía en projectA

            // when
            boolean canEdit = projectMembershipService.canEditEstimationData(
                    projectA.getId(), workerBeta.getUsername()
            );

            // then
            assertThat(canEdit).isFalse();
        }

        @Test
        @DisplayName("worker no asignado no puede gestionar conversiones de esfuerzo")
        void unassignedWorkerCannotManageEffortConversions() {
            // given — workerBeta no tiene membresía en projectA

            // when
            boolean canManage = projectMembershipService.canManageEffortConversions(
                    projectA.getId(), workerBeta.getUsername()
            );

            // then
            assertThat(canManage).isFalse();
        }
    }

    // =========================================================
    // findByProjectId
    // =========================================================

    @Nested
    @DisplayName("Listar membresías de un proyecto")
    class FindByProjectId {

        @Test
        @DisplayName("findByProjectId devuelve el worker asignado al proyecto")
        void findByProjectIdReturnsAssignedWorker() {
            // given
            projectMembershipService.assignOrUpdateWorker(projectA.getId(), workerAlpha.getId(), true, false);

            // when
            List<ProjectMembership> memberships = projectMembershipService.findByProjectId(projectA.getId());

            // then
            assertThat(memberships).hasSize(1);
            assertThat(memberships.get(0).getWorker().getUsername()).isEqualTo(workerAlpha.getUsername());
        }

        @Test
        @DisplayName("findByProjectId devuelve lista vacía si el proyecto no tiene workers asignados")
        void findByProjectIdReturnsEmptyWhenNoWorkers() {
            // given — projectA sin membresías

            // when
            List<ProjectMembership> memberships = projectMembershipService.findByProjectId(projectA.getId());

            // then
            assertThat(memberships).isEmpty();
        }
    }

    // =========================================================
    // findAssignableWorkers
    // =========================================================

    @Test
    @DisplayName("findAssignableWorkers devuelve todos los usuarios con rol ROLE_PROJECT_WORKER")
    void findAssignableWorkersReturnsAllWorkers() {
        // given — workerAlpha y workerBeta creados en setUp

        // when
        List<User> workers = projectMembershipService.findAssignableWorkers();

        // then
        assertThat(workers).extracting(User::getUsername)
                .contains(workerAlpha.getUsername(), workerBeta.getUsername());
        workers.forEach(w ->
                assertThat(w.getRole()).isEqualTo(UserRole.ROLE_PROJECT_WORKER)
        );
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
}
