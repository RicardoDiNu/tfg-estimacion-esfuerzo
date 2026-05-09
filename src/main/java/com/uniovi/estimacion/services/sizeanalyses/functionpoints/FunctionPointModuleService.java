package com.uniovi.estimacion.services.sizeanalyses.functionpoints;

import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.FunctionPointAnalysis;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.modules.FunctionPointModule;
import com.uniovi.estimacion.repositories.sizeanalyses.functionpoints.FunctionPointAnalysisRepository;
import com.uniovi.estimacion.repositories.sizeanalyses.functionpoints.FunctionPointModuleRepository;
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
public class FunctionPointModuleService {

    private final FunctionPointModuleRepository functionPointModuleRepository;
    private final FunctionPointAnalysisRepository functionPointAnalysisRepository;
    private final FunctionPointAnalysisService functionPointAnalysisService;

    public List<FunctionPointModule> findAllByProjectId(Long projectId) {
        List<FunctionPointModule> modules =
                functionPointModuleRepository
                        .findByFunctionPointAnalysisEstimationProjectIdOrderByDisplayOrderAscIdAsc(projectId);

        modules.forEach(this::initializeModuleReferences);

        return modules;
    }

    public Page<FunctionPointModule> findPageByProjectId(Long projectId, Pageable pageable) {
        Page<FunctionPointModule> modulesPage =
                functionPointModuleRepository
                        .findByFunctionPointAnalysisEstimationProjectIdOrderByDisplayOrderAscIdAsc(
                                projectId,
                                pageable
                        );

        modulesPage.getContent().forEach(this::initializeModuleReferences);

        return modulesPage;
    }

    public Optional<FunctionPointModule> findByIdAndProjectId(Long moduleId, Long projectId) {
        return functionPointModuleRepository
                .findByIdAndFunctionPointAnalysisEstimationProjectId(moduleId, projectId)
                .map(module -> {
                    initializeModuleReferences(module);
                    return module;
                });
    }

    @Transactional
    public FunctionPointModule createForProject(EstimationProject project, FunctionPointModule module) {
        FunctionPointAnalysis analysis =
                functionPointAnalysisRepository.findByEstimationProjectId(project.getId())
                        .orElseThrow(() -> new IllegalStateException(
                                "No existe análisis PF para el proyecto " + project.getId()
                        ));

        module.setName(normalize(module.getName()));
        module.setDescription(normalize(module.getDescription()));
        module.setDisplayOrder(nextDisplayOrder(project.getId()));

        analysis.addModule(module);

        FunctionPointModule savedModule = functionPointModuleRepository.save(module);

        functionPointAnalysisService.recalculateAndDeleteDerivedEfforts(analysis);

        return savedModule;
    }

    @Transactional
    public boolean updateBasicData(Long projectId, Long moduleId, FunctionPointModule formModule) {
        Optional<FunctionPointModule> optionalModule = findByIdAndProjectId(moduleId, projectId);

        if (optionalModule.isEmpty()) {
            return false;
        }

        FunctionPointModule existingModule = optionalModule.get();
        existingModule.setName(normalize(formModule.getName()));
        existingModule.setDescription(normalize(formModule.getDescription()));

        return true;
    }

    @Transactional
    public boolean deleteByIdWithContents(Long projectId, Long moduleId) {
        Optional<FunctionPointModule> optionalModule = findByIdAndProjectId(moduleId, projectId);

        if (optionalModule.isEmpty()) {
            return false;
        }

        FunctionPointModule module = optionalModule.get();
        FunctionPointAnalysis analysis = module.getFunctionPointAnalysis();

        if (analysis == null) {
            return false;
        }

        Hibernate.initialize(module.getUserRequirements());
        Hibernate.initialize(analysis.getModules());
        Hibernate.initialize(analysis.getDataFunctions());
        Hibernate.initialize(analysis.getTransactionalFunctions());

        analysis.getDataFunctions().removeIf(dataFunction ->
                dataFunction.getUserRequirement() != null
                        && dataFunction.getUserRequirement().getFunctionPointModule() != null
                        && dataFunction.getUserRequirement().getFunctionPointModule().getId().equals(moduleId)
        );

        analysis.getTransactionalFunctions().removeIf(transactionalFunction ->
                transactionalFunction.getUserRequirement() != null
                        && transactionalFunction.getUserRequirement().getFunctionPointModule() != null
                        && transactionalFunction.getUserRequirement().getFunctionPointModule().getId().equals(moduleId)
        );

        analysis.removeModule(module);

        functionPointAnalysisService.recalculateAndDeleteDerivedEfforts(analysis);
        functionPointAnalysisRepository.save(analysis);

        return true;
    }

    private void initializeModuleReferences(FunctionPointModule module) {
        if (module.getFunctionPointAnalysis() != null) {
            Hibernate.initialize(module.getFunctionPointAnalysis());
        }

        Hibernate.initialize(module.getUserRequirements());
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Integer nextDisplayOrder(Long projectId) {
        return (int) functionPointModuleRepository
                .countByFunctionPointAnalysisEstimationProjectId(projectId) + 1;
    }
}