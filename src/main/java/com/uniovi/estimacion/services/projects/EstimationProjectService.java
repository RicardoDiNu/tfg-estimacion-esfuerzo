package com.uniovi.estimacion.services.projects;

import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.users.User;
import com.uniovi.estimacion.repositories.projects.EstimationProjectRepository;
import com.uniovi.estimacion.repositories.requirements.UserRequirementRepository;
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
    private final FunctionPointAnalysisService functionPointAnalysisService;
    private final UserRequirementRepository userRequirementRepository;
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

        return true;
    }

    @Transactional
    public boolean deleteAccessibleByIdForCurrentUser(Long projectId) {
        Optional<EstimationProject> optionalProject = findAccessibleByIdForCurrentUser(projectId);

        if (optionalProject.isEmpty()) {
            return false;
        }

        // 1) Borrar el análisis PF y, en cascada, sus funciones y GSC
        functionPointAnalysisService.deleteByProjectId(projectId);

        // 2) Borrar los requisitos del proyecto
        userRequirementRepository.deleteAll(
                userRequirementRepository.findByEstimationProjectIdOrderByIdAsc(projectId)
        );

        // 3) Borrar finalmente el proyecto
        estimationProjectRepository.delete(optionalProject.get());
        return true;
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}