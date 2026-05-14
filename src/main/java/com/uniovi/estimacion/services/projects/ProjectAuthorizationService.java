package com.uniovi.estimacion.services.projects;

import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.repositories.projects.EstimationProjectRepository;
import com.uniovi.estimacion.services.users.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectAuthorizationService {

    private final EstimationProjectRepository estimationProjectRepository;
    private final ProjectMembershipService projectMembershipService;
    private final CurrentUserService currentUserService;

    public boolean canAccessProject(Long projectId) {
        if (currentUserService.isAdmin()) {
            return estimationProjectRepository.existsById(projectId);
        }

        Optional<String> optionalUsername = currentUserService.getCurrentUsername();

        if (optionalUsername.isEmpty()) {
            return false;
        }

        String username = optionalUsername.get();

        if (currentUserService.isProjectManager()) {
            return estimationProjectRepository
                    .findByIdAndOwnerUsername(projectId, username)
                    .isPresent();
        }

        if (currentUserService.isProjectWorker()) {
            return projectMembershipService.isWorkerAssignedToProject(projectId, username);
        }

        return false;
    }

    public boolean canManageProject(Long projectId) {
        if (currentUserService.isAdmin()) {
            return estimationProjectRepository.existsById(projectId);
        }

        if (!currentUserService.isProjectManager()) {
            return false;
        }

        return currentUserService.getCurrentUsername()
                .flatMap(username -> estimationProjectRepository.findByIdAndOwnerUsername(projectId, username))
                .isPresent();
    }

    public boolean canEditEstimationData(Long projectId) {
        if (canManageProject(projectId)) {
            return true;
        }

        if (!currentUserService.isProjectWorker()) {
            return false;
        }

        return currentUserService.getCurrentUsername()
                .map(username -> projectMembershipService.canEditEstimationData(projectId, username))
                .orElse(false);
    }

    public boolean canManageEffortConversions(Long projectId) {
        if (canManageProject(projectId)) {
            return true;
        }

        if (!currentUserService.isProjectWorker()) {
            return false;
        }

        return currentUserService.getCurrentUsername()
                .map(username -> projectMembershipService.canManageEffortConversions(projectId, username))
                .orElse(false);
    }

    public Optional<EstimationProject> findAccessibleProject(Long projectId) {
        if (!canAccessProject(projectId)) {
            return Optional.empty();
        }

        return estimationProjectRepository.findById(projectId);
    }

    public Optional<EstimationProject> findManageableProject(Long projectId) {
        if (!canManageProject(projectId)) {
            return Optional.empty();
        }

        return estimationProjectRepository.findById(projectId);
    }

    public boolean canCreateProjects() {
        return currentUserService.isAdminOrProjectManager();
    }
}