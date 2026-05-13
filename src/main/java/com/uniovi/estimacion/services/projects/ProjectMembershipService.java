package com.uniovi.estimacion.services.projects;

import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.projects.ProjectMembership;
import com.uniovi.estimacion.entities.users.User;
import com.uniovi.estimacion.entities.users.UserRole;
import com.uniovi.estimacion.repositories.projects.EstimationProjectRepository;
import com.uniovi.estimacion.repositories.projects.ProjectMembershipRepository;
import com.uniovi.estimacion.repositories.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectMembershipService {

    private final ProjectMembershipRepository projectMembershipRepository;
    private final EstimationProjectRepository estimationProjectRepository;
    private final UserRepository userRepository;

    public List<ProjectMembership> findByProjectId(Long projectId) {
        List<ProjectMembership> memberships =
                projectMembershipRepository.findByProjectIdOrderByWorkerUsernameAsc(projectId);

        memberships.forEach(this::initializeMembershipReferences);

        return memberships;
    }

    public Page<ProjectMembership> findPageByWorkerUsername(String username, Pageable pageable) {
        Page<ProjectMembership> membershipsPage =
                projectMembershipRepository.findByWorkerUsernameOrderByProjectIdAsc(username, pageable);

        membershipsPage.getContent().forEach(this::initializeMembershipReferences);

        return membershipsPage;
    }

    public Page<EstimationProject> findAssignedProjectsByWorkerUsername(String username, Pageable pageable) {
        return projectMembershipRepository.findProjectsByWorkerUsername(username, pageable);
    }

    public Optional<ProjectMembership> findByProjectIdAndWorkerId(Long projectId, Long workerId) {
        return projectMembershipRepository.findByProjectIdAndWorkerId(projectId, workerId)
                .map(membership -> {
                    initializeMembershipReferences(membership);
                    return membership;
                });
    }

    public Optional<ProjectMembership> findByProjectIdAndWorkerUsername(Long projectId, String username) {
        return projectMembershipRepository.findByProjectIdAndWorkerUsername(projectId, username)
                .map(membership -> {
                    initializeMembershipReferences(membership);
                    return membership;
                });
    }

    public boolean isWorkerAssignedToProject(Long projectId, String username) {
        return projectMembershipRepository.existsByProjectIdAndWorkerUsername(projectId, username);
    }

    public boolean canEditEstimationData(Long projectId, String username) {
        return projectMembershipRepository.findByProjectIdAndWorkerUsername(projectId, username)
                .map(ProjectMembership::getCanEditEstimationData)
                .map(Boolean.TRUE::equals)
                .orElse(false);
    }

    public boolean canManageEffortConversions(Long projectId, String username) {
        return projectMembershipRepository.findByProjectIdAndWorkerUsername(projectId, username)
                .map(ProjectMembership::getCanManageEffortConversions)
                .map(Boolean.TRUE::equals)
                .orElse(false);
    }

    @Transactional
    public Optional<ProjectMembership> assignOrUpdateWorker(Long projectId,
                                                            Long workerId,
                                                            Boolean canEditEstimationData,
                                                            Boolean canManageEffortConversions) {
        Optional<EstimationProject> optionalProject =
                estimationProjectRepository.findById(projectId);

        Optional<User> optionalWorker =
                userRepository.findById(workerId);

        if (optionalProject.isEmpty() || optionalWorker.isEmpty()) {
            return Optional.empty();
        }

        User worker = optionalWorker.get();

        if (worker.getRole() != UserRole.ROLE_PROJECT_WORKER) {
            return Optional.empty();
        }

        ProjectMembership membership =
                projectMembershipRepository.findByProjectIdAndWorkerId(projectId, workerId)
                        .orElseGet(ProjectMembership::new);

        membership.setProject(optionalProject.get());
        membership.setWorker(worker);
        membership.setCanEditEstimationData(Boolean.TRUE.equals(canEditEstimationData));
        membership.setCanManageEffortConversions(Boolean.TRUE.equals(canManageEffortConversions));

        ProjectMembership savedMembership = projectMembershipRepository.save(membership);
        initializeMembershipReferences(savedMembership);

        return Optional.of(savedMembership);
    }

    @Transactional
    public boolean removeWorkerFromProject(Long projectId, Long workerId) {
        if (!projectMembershipRepository.existsByProjectIdAndWorkerId(projectId, workerId)) {
            return false;
        }

        projectMembershipRepository.deleteByProjectIdAndWorkerId(projectId, workerId);
        return true;
    }

    @Transactional
    public void deleteAllByProjectId(Long projectId) {
        projectMembershipRepository.deleteByProjectId(projectId);
    }

    public List<User> findAssignableWorkers() {
        return userRepository.findByRoleOrderByUsernameAsc(UserRole.ROLE_PROJECT_WORKER);
    }

    private void initializeMembershipReferences(ProjectMembership membership) {
        Hibernate.initialize(membership.getProject());
        Hibernate.initialize(membership.getWorker());
    }
}