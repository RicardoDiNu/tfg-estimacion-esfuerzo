package com.uniovi.estimacion.services.projects;

import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.repositories.projects.EstimationProjectRepository;
import com.uniovi.estimacion.repositories.requirements.UserRequirementRepository;
import com.uniovi.estimacion.services.functionpoints.FunctionPointAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EstimationProjectService {

    private final EstimationProjectRepository estimationProjectRepository;
    private final FunctionPointAnalysisService functionPointAnalysisService;
    private final UserRequirementRepository userRequirementRepository;

    public Page<EstimationProject> findPage(Pageable pageable) {
        return estimationProjectRepository.findAllByOrderByIdAsc(pageable);
    }

    public Optional<EstimationProject> findById(Long projectId) {
        return estimationProjectRepository.findById(projectId);
    }

    @Transactional
    public EstimationProject create(EstimationProject project) {
        return estimationProjectRepository.save(project);
    }

    @Transactional
    public boolean updateBasicData(Long projectId, EstimationProject formProject) {
        Optional<EstimationProject> optionalProject = estimationProjectRepository.findById(projectId);

        if (optionalProject.isEmpty()) {
            return false;
        }

        EstimationProject existingProject = optionalProject.get();
        existingProject.setName(formProject.getName());
        existingProject.setDescription(formProject.getDescription());

        estimationProjectRepository.save(existingProject);
        return true;
    }

    @Transactional
    public void deleteById(Long projectId) {
        if (!estimationProjectRepository.existsById(projectId)) {
            return;
        }

        // 1) Borrar el análisis PF y, en cascada, sus funciones y GSC
        functionPointAnalysisService.deleteByProjectId(projectId);

        // 2) Borrar los requisitos del proyecto
        userRequirementRepository.deleteAll(
                userRequirementRepository.findByEstimationProjectIdOrderByIdAsc(projectId)
        );

        // 3) Borrar finalmente el proyecto
        estimationProjectRepository.deleteById(projectId);
    }
}