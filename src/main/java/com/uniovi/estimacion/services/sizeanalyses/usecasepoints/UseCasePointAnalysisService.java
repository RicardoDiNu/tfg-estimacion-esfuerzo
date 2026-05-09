package com.uniovi.estimacion.services.sizeanalyses.usecasepoints;

import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.UseCasePointAnalysis;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.actors.UseCaseActor;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.factors.EnvironmentalFactorAssessment;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.factors.EnvironmentalFactorType;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.factors.TechnicalFactorAssessment;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.factors.TechnicalFactorType;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.modules.UseCasePointModule;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.usecases.UseCaseEntry;
import com.uniovi.estimacion.repositories.sizeanalyses.usecasepoints.UseCaseActorRepository;
import com.uniovi.estimacion.repositories.sizeanalyses.usecasepoints.UseCaseEntryRepository;
import com.uniovi.estimacion.repositories.sizeanalyses.usecasepoints.UseCasePointAnalysisRepository;
import com.uniovi.estimacion.repositories.sizeanalyses.usecasepoints.UseCasePointModuleRepository;
import com.uniovi.estimacion.services.effortconversions.delphi.EffortResultsInvalidationCoordinator;
import com.uniovi.estimacion.web.forms.sizeanalyses.usecasepoints.UseCaseEnvironmentalFactorsForm;
import com.uniovi.estimacion.web.forms.sizeanalyses.usecasepoints.UseCaseFactorAssessmentForm;
import com.uniovi.estimacion.web.forms.sizeanalyses.usecasepoints.UseCaseTechnicalFactorsForm;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UseCasePointAnalysisService {

    private final UseCasePointAnalysisRepository useCasePointAnalysisRepository;
    private final UseCaseActorRepository useCaseActorRepository;
    private final UseCaseEntryRepository useCaseEntryRepository;
    private final UseCasePointModuleRepository useCasePointModuleRepository;
    private final UseCasePointCalculationService useCasePointCalculationService;
    private final EffortResultsInvalidationCoordinator effortResultsInvalidationCoordinator;

    public Optional<UseCasePointAnalysis> findByProjectId(Long projectId) {
        return useCasePointAnalysisRepository.findByEstimationProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public Optional<UseCasePointAnalysis> findDetailedByProjectId(Long projectId) {
        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisRepository.findByEstimationProjectId(projectId);

        optionalAnalysis.ifPresent(this::initializeAnalysisCollections);

        return optionalAnalysis;
    }

    @Transactional
    public void createInitialAnalysis(EstimationProject estimationProject, String systemBoundaryDescription) {
        UseCasePointAnalysis analysis =
                new UseCasePointAnalysis(estimationProject, normalizeText(systemBoundaryDescription));

        initializeTechnicalFactors(analysis);
        initializeEnvironmentalFactors(analysis);

        useCasePointCalculationService.recalculateAnalysis(analysis);
        useCasePointAnalysisRepository.save(analysis);
    }

    @Transactional
    public boolean updateSystemBoundaryDescription(Long projectId, String systemBoundaryDescription) {
        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisRepository.findByEstimationProjectId(projectId);

        if (optionalAnalysis.isEmpty()) {
            return false;
        }

        String normalizedBoundary = normalizeText(systemBoundaryDescription);

        if (normalizedBoundary == null || normalizedBoundary.isBlank()) {
            return false;
        }

        UseCasePointAnalysis analysis = optionalAnalysis.get();
        analysis.setSystemBoundaryDescription(normalizedBoundary);

        recalculateManagedAnalysis(analysis);

        return true;
    }

    @Transactional
    public void deleteByProjectId(Long projectId) {
        useCasePointAnalysisRepository.findByEstimationProjectId(projectId)
                .ifPresent(useCasePointAnalysisRepository::delete);
    }

    public Page<UseCasePointModule> findModulesPageByProjectId(Long projectId, Pageable pageable) {
        Page<UseCasePointModule> page =
                useCasePointModuleRepository
                        .findByUseCasePointAnalysisEstimationProjectIdOrderByIdAsc(projectId, pageable);

        page.getContent().forEach(this::initializeModuleReferences);

        return page;
    }

    public Page<UseCaseEntry> findUseCasesPageByModuleId(Long moduleId, Pageable pageable) {
        Page<UseCaseEntry> page =
                useCaseEntryRepository.findByUseCasePointModuleIdOrderByIdAsc(moduleId, pageable);

        page.getContent().forEach(this::initializeUseCaseReferences);

        return page;
    }

    public List<UseCasePointModule> findAllModulesByProjectId(Long projectId) {
        List<UseCasePointModule> modules =
                useCasePointModuleRepository
                        .findByUseCasePointAnalysisEstimationProjectIdOrderByIdAsc(projectId);

        modules.forEach(this::initializeModuleReferences);

        return modules;
    }

    @Transactional(readOnly = true)
    public Optional<UseCasePointModule> findModule(Long projectId, Long moduleId) {
        Optional<UseCasePointModule> optionalModule =
                useCasePointModuleRepository
                        .findByIdAndUseCasePointAnalysisEstimationProjectId(moduleId, projectId);

        optionalModule.ifPresent(this::initializeModuleReferences);

        return optionalModule;
    }

    @Transactional
    public Optional<UseCasePointModule> addModule(Long projectId, UseCasePointModule module) {
        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisRepository.findByEstimationProjectId(projectId);

        if (optionalAnalysis.isEmpty()) {
            return Optional.empty();
        }

        UseCasePointAnalysis analysis = optionalAnalysis.get();

        module.setName(normalizeText(module.getName()));
        module.setDescription(normalizeText(module.getDescription()));
        module.setUseCasePointAnalysis(analysis);

        analysis.getModules().add(module);

        recalculateManagedAnalysis(analysis);

        return Optional.of(module);
    }

    @Transactional
    public boolean updateModule(Long projectId, Long moduleId, UseCasePointModule formModule) {
        Optional<UseCasePointModule> optionalModule =
                useCasePointModuleRepository
                        .findByIdAndUseCasePointAnalysisEstimationProjectId(moduleId, projectId);

        if (optionalModule.isEmpty()) {
            return false;
        }

        UseCasePointModule existingModule = optionalModule.get();

        existingModule.setName(normalizeText(formModule.getName()));
        existingModule.setDescription(normalizeText(formModule.getDescription()));

        UseCasePointAnalysis analysis = existingModule.getUseCasePointAnalysis();
        recalculateManagedAnalysis(analysis);

        return true;
    }

    @Transactional
    public boolean deleteModule(Long projectId, Long moduleId) {
        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisRepository.findByEstimationProjectId(projectId);

        if (optionalAnalysis.isEmpty()) {
            return false;
        }

        UseCasePointAnalysis analysis = optionalAnalysis.get();
        initializeAnalysisCollections(analysis);

        Optional<UseCasePointModule> optionalModule = analysis.getModules().stream()
                .filter(module -> module.getId().equals(moduleId))
                .findFirst();

        if (optionalModule.isEmpty()) {
            return false;
        }

        UseCasePointModule module = optionalModule.get();

        analysis.getUseCases().removeIf(useCase ->
                useCase.getUseCasePointModule() != null
                        && useCase.getUseCasePointModule().getId().equals(moduleId)
        );

        analysis.getModules().remove(module);

        recalculateManagedAnalysis(analysis);

        return true;
    }

    public Page<UseCaseActor> findActorsPageByProjectId(Long projectId, Pageable pageable) {
        return useCaseActorRepository
                .findByUseCasePointAnalysisEstimationProjectIdOrderByIdAsc(projectId, pageable);
    }

    public List<UseCaseActor> findAllActorsByProjectId(Long projectId) {
        return useCaseActorRepository
                .findByUseCasePointAnalysisEstimationProjectIdOrderByIdAsc(projectId);
    }

    @Transactional(readOnly = true)
    public Optional<UseCaseActor> findActor(Long projectId, Long actorId) {
        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisRepository.findByEstimationProjectId(projectId);

        if (optionalAnalysis.isEmpty()) {
            return Optional.empty();
        }

        UseCasePointAnalysis analysis = optionalAnalysis.get();
        Hibernate.initialize(analysis.getActors());

        return analysis.getActors().stream()
                .filter(actor -> actor.getId().equals(actorId))
                .findFirst();
    }

    @Transactional
    public boolean addActor(Long projectId, UseCaseActor actor) {
        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisRepository.findByEstimationProjectId(projectId);

        if (optionalAnalysis.isEmpty()) {
            return false;
        }

        UseCasePointAnalysis analysis = optionalAnalysis.get();

        actor.setName(normalizeText(actor.getName()));
        actor.setDescription(normalizeText(actor.getDescription()));
        actor.setWeight(useCasePointCalculationService.calculateActorWeight(actor.getComplexity()));
        actor.setUseCasePointAnalysis(analysis);

        analysis.getActors().add(actor);

        recalculateManagedAnalysis(analysis);

        return true;
    }

    @Transactional
    public boolean updateActor(Long projectId, Long actorId, UseCaseActor formActor) {
        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisRepository.findByEstimationProjectId(projectId);

        if (optionalAnalysis.isEmpty()) {
            return false;
        }

        UseCasePointAnalysis analysis = optionalAnalysis.get();
        Hibernate.initialize(analysis.getActors());

        Optional<UseCaseActor> optionalActor = analysis.getActors().stream()
                .filter(actor -> actor.getId().equals(actorId))
                .findFirst();

        if (optionalActor.isEmpty()) {
            return false;
        }

        UseCaseActor existingActor = optionalActor.get();

        existingActor.setName(normalizeText(formActor.getName()));
        existingActor.setDescription(normalizeText(formActor.getDescription()));
        existingActor.setComplexity(formActor.getComplexity());
        existingActor.setWeight(useCasePointCalculationService.calculateActorWeight(formActor.getComplexity()));

        recalculateManagedAnalysis(analysis);

        return true;
    }

    @Transactional
    public boolean deleteActor(Long projectId, Long actorId) {
        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisRepository.findByEstimationProjectId(projectId);

        if (optionalAnalysis.isEmpty()) {
            return false;
        }

        UseCasePointAnalysis analysis = optionalAnalysis.get();
        Hibernate.initialize(analysis.getActors());

        boolean removed = analysis.getActors()
                .removeIf(actor -> actor.getId().equals(actorId));

        if (removed) {
            recalculateManagedAnalysis(analysis);
        }

        return removed;
    }

    public Page<UseCaseEntry> findUseCasesPageByProjectId(Long projectId, Pageable pageable) {
        Page<UseCaseEntry> page =
                useCaseEntryRepository
                        .findByUseCasePointAnalysisEstimationProjectIdOrderByIdAsc(projectId, pageable);

        page.getContent().forEach(this::initializeUseCaseReferences);

        return page;
    }

    public List<UseCaseEntry> findAllUseCasesByProjectId(Long projectId) {
        List<UseCaseEntry> useCases =
                useCaseEntryRepository
                        .findByUseCasePointAnalysisEstimationProjectIdOrderByIdAsc(projectId);

        useCases.forEach(this::initializeUseCaseReferences);

        return useCases;
    }

    public List<UseCaseEntry> findAllUseCasesByModuleId(Long moduleId) {
        List<UseCaseEntry> useCases =
                useCaseEntryRepository.findByUseCasePointModuleIdOrderByIdAsc(moduleId);

        useCases.forEach(this::initializeUseCaseReferences);

        return useCases;
    }

    @Transactional(readOnly = true)
    public Optional<UseCaseEntry> findUseCase(Long projectId, Long useCaseId) {
        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisRepository.findByEstimationProjectId(projectId);

        if (optionalAnalysis.isEmpty()) {
            return Optional.empty();
        }

        UseCasePointAnalysis analysis = optionalAnalysis.get();
        Hibernate.initialize(analysis.getUseCases());

        Optional<UseCaseEntry> optionalUseCase = analysis.getUseCases().stream()
                .filter(useCase -> useCase.getId().equals(useCaseId))
                .findFirst();

        optionalUseCase.ifPresent(this::initializeUseCaseReferences);

        return optionalUseCase;
    }

    @Transactional
    public boolean addUseCaseToModule(Long projectId,
                                      Long moduleId,
                                      UseCaseEntry useCase,
                                      List<Long> actorIds) {
        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisRepository.findByEstimationProjectId(projectId);

        Optional<UseCasePointModule> optionalModule =
                useCasePointModuleRepository
                        .findByIdAndUseCasePointAnalysisEstimationProjectId(moduleId, projectId);

        if (optionalAnalysis.isEmpty() || optionalModule.isEmpty()) {
            return false;
        }

        UseCasePointAnalysis analysis = optionalAnalysis.get();
        UseCasePointModule module = optionalModule.get();

        Hibernate.initialize(analysis.getActors());

        List<UseCaseActor> selectedActors = resolveSelectedActors(analysis, actorIds);

        if (selectedActors.isEmpty()) {
            return false;
        }

        useCase.setUseCasePointAnalysis(analysis);
        applyUseCaseData(useCase, useCase, selectedActors);

        if (useCase.getComplexity() == null || useCase.getWeight() == null) {
            return false;
        }

        module.addUseCase(useCase);
        analysis.getUseCases().add(useCase);

        recalculateManagedAnalysis(analysis);

        return true;
    }

    @Transactional
    public boolean updateUseCase(Long projectId,
                                 Long moduleId,
                                 Long useCaseId,
                                 UseCaseEntry formUseCase,
                                 List<Long> actorIds) {
        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisRepository.findByEstimationProjectId(projectId);

        Optional<UseCasePointModule> optionalModule =
                useCasePointModuleRepository
                        .findByIdAndUseCasePointAnalysisEstimationProjectId(moduleId, projectId);

        if (optionalAnalysis.isEmpty() || optionalModule.isEmpty()) {
            return false;
        }

        UseCasePointAnalysis analysis = optionalAnalysis.get();
        Hibernate.initialize(analysis.getUseCases());
        Hibernate.initialize(analysis.getActors());

        Optional<UseCaseEntry> optionalUseCase = analysis.getUseCases().stream()
                .filter(useCase -> useCase.getId().equals(useCaseId))
                .filter(useCase -> useCase.getUseCasePointModule() != null
                        && useCase.getUseCasePointModule().getId().equals(moduleId))
                .findFirst();

        if (optionalUseCase.isEmpty()) {
            return false;
        }

        List<UseCaseActor> selectedActors = resolveSelectedActors(analysis, actorIds);

        if (selectedActors.isEmpty()) {
            return false;
        }

        UseCaseEntry existingUseCase = optionalUseCase.get();
        Hibernate.initialize(existingUseCase.getActors());

        applyUseCaseData(existingUseCase, formUseCase, selectedActors);

        if (existingUseCase.getComplexity() == null || existingUseCase.getWeight() == null) {
            return false;
        }

        recalculateManagedAnalysis(analysis);

        return true;
    }

    @Transactional
    public boolean deleteUseCase(Long projectId, Long moduleId, Long useCaseId) {
        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisRepository.findByEstimationProjectId(projectId);

        Optional<UseCasePointModule> optionalModule =
                useCasePointModuleRepository
                        .findByIdAndUseCasePointAnalysisEstimationProjectId(moduleId, projectId);

        if (optionalAnalysis.isEmpty() || optionalModule.isEmpty()) {
            return false;
        }

        UseCasePointAnalysis analysis = optionalAnalysis.get();
        UseCasePointModule module = optionalModule.get();

        initializeAnalysisCollections(analysis);
        initializeModuleReferences(module);

        Optional<UseCaseEntry> optionalUseCase = module.getUseCases().stream()
                .filter(useCase -> useCase.getId().equals(useCaseId))
                .findFirst();

        if (optionalUseCase.isEmpty()) {
            return false;
        }

        UseCaseEntry useCase = optionalUseCase.get();

        module.removeUseCase(useCase);
        analysis.getUseCases().remove(useCase);

        recalculateManagedAnalysis(analysis);

        return true;
    }

    @Transactional
    public boolean updateTechnicalFactors(Long projectId, UseCaseTechnicalFactorsForm form) {
        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisRepository.findByEstimationProjectId(projectId);

        if (optionalAnalysis.isEmpty()) {
            return false;
        }

        UseCasePointAnalysis analysis = optionalAnalysis.get();
        Hibernate.initialize(analysis.getTechnicalFactorAssessments());

        for (TechnicalFactorAssessment existing : analysis.getTechnicalFactorAssessments()) {
            form.getAssessments().stream()
                    .filter(incoming -> existing.getFactorType().name().equals(incoming.getFactorCode()))
                    .findFirst()
                    .ifPresent(incoming ->
                            existing.setDegreeOfInfluence(
                                    normalizeDegreeOfInfluence(incoming.getDegreeOfInfluence())
                            )
                    );
        }

        recalculateManagedAnalysis(analysis);

        return true;
    }

    @Transactional
    public boolean updateEnvironmentalFactors(Long projectId, UseCaseEnvironmentalFactorsForm form) {
        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisRepository.findByEstimationProjectId(projectId);

        if (optionalAnalysis.isEmpty()) {
            return false;
        }

        UseCasePointAnalysis analysis = optionalAnalysis.get();
        Hibernate.initialize(analysis.getEnvironmentalFactorAssessments());

        for (EnvironmentalFactorAssessment existing : analysis.getEnvironmentalFactorAssessments()) {
            form.getAssessments().stream()
                    .filter(incoming -> existing.getFactorType().name().equals(incoming.getFactorCode()))
                    .findFirst()
                    .ifPresent(incoming ->
                            existing.setDegreeOfInfluence(
                                    normalizeDegreeOfInfluence(incoming.getDegreeOfInfluence())
                            )
                    );
        }

        recalculateManagedAnalysis(analysis);

        return true;
    }

    public UseCaseTechnicalFactorsForm buildTechnicalFactorsForm(UseCasePointAnalysis analysis) {
        initializeAnalysisCollections(analysis);

        UseCaseTechnicalFactorsForm form = new UseCaseTechnicalFactorsForm();
        form.setAssessments(new ArrayList<>());

        for (TechnicalFactorAssessment assessment : analysis.getTechnicalFactorAssessments()) {
            UseCaseFactorAssessmentForm assessmentForm = new UseCaseFactorAssessmentForm();
            assessmentForm.setFactorCode(assessment.getFactorType().name());
            assessmentForm.setDegreeOfInfluence(assessment.getDegreeOfInfluence());

            form.getAssessments().add(assessmentForm);
        }

        return form;
    }

    public UseCaseEnvironmentalFactorsForm buildEnvironmentalFactorsForm(UseCasePointAnalysis analysis) {
        initializeAnalysisCollections(analysis);

        UseCaseEnvironmentalFactorsForm form = new UseCaseEnvironmentalFactorsForm();
        form.setAssessments(new ArrayList<>());

        for (EnvironmentalFactorAssessment assessment : analysis.getEnvironmentalFactorAssessments()) {
            UseCaseFactorAssessmentForm assessmentForm = new UseCaseFactorAssessmentForm();
            assessmentForm.setFactorCode(assessment.getFactorType().name());
            assessmentForm.setDegreeOfInfluence(assessment.getDegreeOfInfluence());

            form.getAssessments().add(assessmentForm);
        }

        return form;
    }

    @Transactional
    public void recalculateAndDeleteDerivedEfforts(UseCasePointAnalysis analysis) {
        useCasePointCalculationService.recalculateAnalysis(analysis);
        effortResultsInvalidationCoordinator.invalidateForSizeAnalysis(analysis);
    }

    private void initializeAnalysisCollections(UseCasePointAnalysis analysis) {
        Hibernate.initialize(analysis.getActors());
        Hibernate.initialize(analysis.getModules());
        Hibernate.initialize(analysis.getUseCases());
        Hibernate.initialize(analysis.getTechnicalFactorAssessments());
        Hibernate.initialize(analysis.getEnvironmentalFactorAssessments());

        analysis.getModules().forEach(this::initializeModuleReferences);
        analysis.getUseCases().forEach(this::initializeUseCaseReferences);

        analysis.getTechnicalFactorAssessments()
                .sort(Comparator.comparingInt(assessment -> assessment.getFactorType().ordinal()));

        analysis.getEnvironmentalFactorAssessments()
                .sort(Comparator.comparingInt(assessment -> assessment.getFactorType().ordinal()));
    }

    private void initializeModuleReferences(UseCasePointModule module) {
        Hibernate.initialize(module.getUseCases());
        module.getUseCases().forEach(this::initializeUseCaseReferences);
    }

    private void initializeUseCaseReferences(UseCaseEntry useCase) {
        if (useCase.getUseCasePointModule() != null) {
            Hibernate.initialize(useCase.getUseCasePointModule());
        }

        Hibernate.initialize(useCase.getActors());
    }

    private void initializeTechnicalFactors(UseCasePointAnalysis analysis) {
        for (TechnicalFactorType type : TechnicalFactorType.values()) {
            TechnicalFactorAssessment assessment = new TechnicalFactorAssessment();
            assessment.setUseCasePointAnalysis(analysis);
            assessment.setFactorType(type);
            assessment.setDegreeOfInfluence(0);

            analysis.getTechnicalFactorAssessments().add(assessment);
        }
    }

    private void initializeEnvironmentalFactors(UseCasePointAnalysis analysis) {
        for (EnvironmentalFactorType type : EnvironmentalFactorType.values()) {
            EnvironmentalFactorAssessment assessment = new EnvironmentalFactorAssessment();
            assessment.setUseCasePointAnalysis(analysis);
            assessment.setFactorType(type);
            assessment.setDegreeOfInfluence(0);

            analysis.getEnvironmentalFactorAssessments().add(assessment);
        }
    }

    private Integer normalizeDegreeOfInfluence(Integer value) {
        if (value == null || value < 0) {
            return 0;
        }

        return Math.min(value, 5);
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }

    private void recalculateManagedAnalysis(UseCasePointAnalysis analysis) {
        recalculateAndDeleteDerivedEfforts(analysis);
    }

    private void applyUseCaseData(UseCaseEntry target,
                                  UseCaseEntry source,
                                  List<UseCaseActor> selectedActors) {
        target.setName(normalizeText(source.getName()));
        target.setDescription(normalizeText(source.getDescription()));

        target.setTriggerCondition(normalizeText(source.getTriggerCondition()));
        target.setPreconditions(normalizeText(source.getPreconditions()));
        target.setPostconditions(normalizeText(source.getPostconditions()));
        target.setNormalFlow(normalizeText(source.getNormalFlow()));
        target.setAlternativeFlows(normalizeText(source.getAlternativeFlows()));
        target.setExceptionFlows(normalizeText(source.getExceptionFlows()));

        target.setTransactionCount(source.getTransactionCount());

        target.setComplexity(
                useCasePointCalculationService.determineUseCaseComplexity(source.getTransactionCount())
        );

        target.setWeight(
                useCasePointCalculationService.calculateUseCaseWeightFromTransactionCount(source.getTransactionCount())
        );

        target.getActors().clear();
        target.getActors().addAll(selectedActors);
    }

    private List<UseCaseActor> resolveSelectedActors(UseCasePointAnalysis analysis,
                                                     List<Long> actorIds) {
        if (actorIds == null || actorIds.isEmpty()) {
            return List.of();
        }

        Set<Long> selectedIds = actorIds.stream()
                .filter(id -> id != null)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        if (selectedIds.isEmpty()) {
            return List.of();
        }

        return analysis.getActors().stream()
                .filter(actor -> actor.getId() != null)
                .filter(actor -> selectedIds.contains(actor.getId()))
                .toList();
    }
}