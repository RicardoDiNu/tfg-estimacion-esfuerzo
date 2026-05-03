package com.uniovi.estimacion.services.projects;

import com.uniovi.estimacion.entities.functionpoints.FunctionPointAnalysis;
import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.users.User;
import com.uniovi.estimacion.repositories.functionpoints.FunctionPointAnalysisRepository;
import com.uniovi.estimacion.repositories.projects.EstimationModuleRepository;
import com.uniovi.estimacion.repositories.projects.EstimationProjectRepository;
import com.uniovi.estimacion.services.functionpoints.FunctionPointAnalysisService;
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
    private final EstimationModuleRepository estimationModuleRepository;
    private final FunctionPointAnalysisRepository functionPointAnalysisRepository;
    private final FunctionPointAnalysisService functionPointAnalysisService;
    private final CurrentUserService currentUserService;

    public Page<EstimationProject> findPageForCurrentUser(Pageable pageable) {
        if (currentUserService.isAdmin()) {
            return estimationProjectRepository.findAllByOrderByIdAsc(pageable);
        }

        return currentUserService.getCurrentUsername()
                .map(username -> estimationProjectRepository.findByOwnerUsernameOrderByIdAsc(username, pageable))
                .orElse(Page.empty(pageable));
    }

    public Optional<EstimationProject> findAccessibleByIdForCurrentUser(Long projectId) {
        if (currentUserService.isAdmin()) {
            return estimationProjectRepository.findById(projectId);
        }

        return currentUserService.getCurrentUsername()
                .flatMap(username -> estimationProjectRepository.findByIdAndOwnerUsername(projectId, username));
    }

    public Optional<EstimationProject> findById(Long projectId) {
        return estimationProjectRepository.findById(projectId);
    }

    @Transactional
    public EstimationProject create(EstimationProject project) {
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
        Optional<EstimationProject> optionalProject = findAccessibleByIdForCurrentUser(projectId);

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
        Optional<EstimationProject> optionalProject = findAccessibleByIdForCurrentUser(projectId);

        if (optionalProject.isEmpty()) {
            return false;
        }

        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisRepository.findByEstimationProjectId(projectId);

        if (optionalAnalysis.isPresent()) {
            functionPointAnalysisService.deleteByProjectId(projectId);
        }

        estimationModuleRepository.deleteAll(
                estimationModuleRepository.findByEstimationProjectIdOrderByIdAsc(projectId)
        );

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