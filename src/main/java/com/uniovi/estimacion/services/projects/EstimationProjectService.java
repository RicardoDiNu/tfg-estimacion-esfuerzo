package com.uniovi.estimacion.services.projects;

import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.users.User;
import com.uniovi.estimacion.repositories.projects.EstimationProjectRepository;
import com.uniovi.estimacion.repositories.sizeanalyses.functionpoints.FunctionPointAnalysisRepository;
import com.uniovi.estimacion.repositories.sizeanalyses.usecasepoints.UseCasePointAnalysisRepository;
import com.uniovi.estimacion.services.sizeanalyses.functionpoints.FunctionPointAnalysisService;
import com.uniovi.estimacion.services.sizeanalyses.usecasepoints.UseCasePointAnalysisService;
import com.uniovi.estimacion.services.users.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EstimationProjectService {

    private final EstimationProjectRepository estimationProjectRepository;

    private final FunctionPointAnalysisRepository functionPointAnalysisRepository;
    private final FunctionPointAnalysisService functionPointAnalysisService;

    private final UseCasePointAnalysisRepository useCasePointAnalysisRepository;
    private final UseCasePointAnalysisService useCasePointAnalysisService;

    private final ProjectMembershipService projectMembershipService;
    private final CurrentUserService currentUserService;

    public Page<EstimationProject> findPageForCurrentUser(Pageable pageable) {
        if (currentUserService.isAdmin()) {
            return estimationProjectRepository.findAllByOrderByIdAsc(pageable);
        }

        if (currentUserService.isProjectManager()) {
            return currentUserService.getCurrentUsername()
                    .map(username -> estimationProjectRepository.findByOwnerUsernameOrderByIdAsc(username, pageable))
                    .orElse(Page.empty(pageable));
        }

        if (currentUserService.isProjectWorker()) {
            return currentUserService.getCurrentUsername()
                    .map(username -> projectMembershipService.findAssignedProjectsByWorkerUsername(username, pageable))
                    .orElse(Page.empty(pageable));
        }

        return Page.empty(pageable);
    }

    public Optional<EstimationProject> findAccessibleByIdForCurrentUser(Long projectId) {
        if (currentUserService.isAdmin()) {
            return estimationProjectRepository.findById(projectId);
        }

        if (currentUserService.isProjectManager()) {
            return currentUserService.getCurrentUsername()
                    .flatMap(username -> estimationProjectRepository.findByIdAndOwnerUsername(projectId, username));
        }

        if (currentUserService.isProjectWorker()) {
            return currentUserService.getCurrentUsername()
                    .filter(username -> projectMembershipService.isWorkerAssignedToProject(projectId, username))
                    .flatMap(username -> estimationProjectRepository.findById(projectId));
        }

        return Optional.empty();
    }

    public Optional<EstimationProject> findManageableByIdForCurrentUser(Long projectId) {
        if (currentUserService.isAdmin()) {
            return estimationProjectRepository.findById(projectId);
        }

        if (currentUserService.isProjectManager()) {
            return currentUserService.getCurrentUsername()
                    .flatMap(username -> estimationProjectRepository.findByIdAndOwnerUsername(projectId, username));
        }

        return Optional.empty();
    }

    public Optional<EstimationProject> findById(Long projectId) {
        return estimationProjectRepository.findById(projectId);
    }

    @Transactional
    public EstimationProject create(EstimationProject project) {
        if (!currentUserService.isAdminOrProjectManager()) {
            throw new IllegalStateException("El usuario actual no puede crear proyectos");
        }

        User currentUser = currentUserService.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("No hay usuario autenticado"));

        project.setName(normalize(project.getName()));
        project.setDescription(normalize(project.getDescription()));
        project.setOwner(currentUser);
        normalizeCostFields(project);

        return estimationProjectRepository.save(project);
    }

    @Transactional
    public boolean updateBasicDataForCurrentUser(Long projectId, EstimationProject formProject) {
        Optional<EstimationProject> optionalProject = findManageableByIdForCurrentUser(projectId);

        if (optionalProject.isEmpty()) {
            return false;
        }

        EstimationProject existingProject = optionalProject.get();

        existingProject.setName(normalize(formProject.getName()));
        existingProject.setDescription(normalize(formProject.getDescription()));

        existingProject.setHourlyRate(formProject.getHourlyRate());
        existingProject.setCurrencyCode(formProject.getCurrencyCode());
        normalizeCostFields(existingProject);

        estimationProjectRepository.save(existingProject);

        return true;
    }

    @Transactional
    public boolean deleteAccessibleByIdForCurrentUser(Long projectId) {
        Optional<EstimationProject> optionalProject = findManageableByIdForCurrentUser(projectId);

        if (optionalProject.isEmpty()) {
            return false;
        }

        if (functionPointAnalysisRepository.findByEstimationProjectId(projectId).isPresent()) {
            functionPointAnalysisService.deleteByProjectId(projectId);
        }

        if (useCasePointAnalysisRepository.findByEstimationProjectId(projectId).isPresent()) {
            useCasePointAnalysisService.deleteByProjectId(projectId);
        }

        estimationProjectRepository.delete(optionalProject.get());
        estimationProjectRepository.flush();

        return true;
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }

    private void normalizeCostFields(EstimationProject project) {
        if (project.getCurrencyCode() == null || project.getCurrencyCode().trim().isEmpty()) {
            project.setCurrencyCode("EUR");
        } else {
            project.setCurrencyCode(project.getCurrencyCode().trim().toUpperCase());
        }
    }
}