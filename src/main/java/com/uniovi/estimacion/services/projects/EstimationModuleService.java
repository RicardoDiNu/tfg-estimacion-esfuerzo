package com.uniovi.estimacion.services.projects;

import com.uniovi.estimacion.entities.functionpoints.FunctionPointAnalysis;
import com.uniovi.estimacion.entities.projects.EstimationModule;
import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.repositories.functionpoints.FunctionPointAnalysisRepository;
import com.uniovi.estimacion.repositories.projects.EstimationModuleRepository;
import com.uniovi.estimacion.services.functionpoints.FunctionPointAnalysisService;
import com.uniovi.estimacion.services.functionpoints.FunctionPointCalculationService;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EstimationModuleService {

    private final EstimationModuleRepository estimationModuleRepository;
    private final FunctionPointAnalysisRepository functionPointAnalysisRepository;
    private final FunctionPointAnalysisService functionPointAnalysisService;

    public List<EstimationModule> findAllByProjectId(Long projectId) {
        return estimationModuleRepository.findByEstimationProjectIdOrderByDisplayOrderAscIdAsc(projectId);
    }

    public Optional<EstimationModule> findByIdAndProjectId(Long moduleId, Long projectId) {
        return estimationModuleRepository.findByIdAndEstimationProjectId(moduleId, projectId);
    }

    @Transactional
    public EstimationModule createForProject(EstimationProject project, EstimationModule module) {
        module.setEstimationProject(project);
        module.setName(normalize(module.getName()));
        module.setDescription(normalize(module.getDescription()));
        return estimationModuleRepository.save(module);
    }

    @Transactional
    public boolean deleteByIdWithContents(Long projectId, Long moduleId) {
        Optional<EstimationModule> optionalModule = findByIdAndProjectId(moduleId, projectId);
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisRepository.findByEstimationProjectId(projectId);

        if (optionalModule.isEmpty()) {
            return false;
        }

        EstimationModule module = optionalModule.get();

        if (optionalAnalysis.isPresent()) {
            FunctionPointAnalysis analysis = optionalAnalysis.get();

            Hibernate.initialize(module.getUserRequirements());
            Hibernate.initialize(analysis.getDataFunctions());
            Hibernate.initialize(analysis.getTransactionalFunctions());

            analysis.getDataFunctions().removeIf(dataFunction ->
                    dataFunction.getUserRequirement() != null
                            && dataFunction.getUserRequirement().getEstimationModule() != null
                            && dataFunction.getUserRequirement().getEstimationModule().getId().equals(moduleId));

            analysis.getTransactionalFunctions().removeIf(transactionalFunction ->
                    transactionalFunction.getUserRequirement() != null
                            && transactionalFunction.getUserRequirement().getEstimationModule() != null
                            && transactionalFunction.getUserRequirement().getEstimationModule().getId().equals(moduleId));

            functionPointAnalysisService.recalculateAndDeleteDerivedEfforts(analysis);
            functionPointAnalysisRepository.save(analysis);
        }

        estimationModuleRepository.delete(module);
        return true;
    }


    @Transactional
    public boolean updateBasicData(Long projectId, Long moduleId, EstimationModule formModule) {
        Optional<EstimationModule> optionalModule = findByIdAndProjectId(moduleId, projectId);

        if (optionalModule.isEmpty()) {
            return false;
        }

        EstimationModule existingModule = optionalModule.get();
        existingModule.setName(normalize(formModule.getName()));
        existingModule.setDescription(normalize(formModule.getDescription()));

        estimationModuleRepository.save(existingModule);
        return true;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private Integer nextDisplayOrder(Long projectId) {
        return (int) estimationModuleRepository.countByEstimationProjectId(projectId) + 1;
    }

    public Page<EstimationModule> findPageByProjectId(Long projectId, Pageable pageable) {
        return estimationModuleRepository.findByEstimationProjectIdOrderByIdAsc(projectId, pageable);
    }
}