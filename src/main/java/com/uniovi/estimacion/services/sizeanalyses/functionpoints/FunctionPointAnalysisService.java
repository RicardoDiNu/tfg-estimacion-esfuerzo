package com.uniovi.estimacion.services.sizeanalyses.functionpoints;

import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.FunctionPointAnalysis;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.DataFunction;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.FunctionPointComplexity;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.TransactionalFunction;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.gscs.GeneralSystemCharacteristicAssessment;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.gscs.GeneralSystemCharacteristicType;
import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.requirements.UserRequirement;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.weights.FunctionPointFunctionType;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.weights.FunctionPointWeightMatrixEntry;
import com.uniovi.estimacion.repositories.sizeanalyses.functionpoints.DataFunctionRepository;
import com.uniovi.estimacion.repositories.sizeanalyses.functionpoints.FunctionPointAnalysisRepository;
import com.uniovi.estimacion.repositories.sizeanalyses.functionpoints.TransactionalFunctionRepository;
import com.uniovi.estimacion.repositories.sizeanalyses.functionpoints.UserRequirementRepository;
import com.uniovi.estimacion.repositories.sizeanalyses.functionpoints.FunctionPointModuleRepository;
import com.uniovi.estimacion.services.effortconversions.delphi.EffortResultsInvalidationCoordinator;
import com.uniovi.estimacion.web.forms.sizeanalyses.functionpoints.FunctionPointWeightMatrixForm;
import com.uniovi.estimacion.web.forms.sizeanalyses.functionpoints.FunctionPointWeightMatrixRowForm;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FunctionPointAnalysisService {

    private final FunctionPointAnalysisRepository functionPointAnalysisRepository;
    private final DataFunctionRepository dataFunctionRepository;
    private final TransactionalFunctionRepository transactionalFunctionRepository;
    private final UserRequirementRepository userRequirementRepository;
    private final FunctionPointCalculationService functionPointCalculationService;
    private final FunctionPointModuleRepository functionPointModuleRepository;
    private final EffortResultsInvalidationCoordinator effortResultsInvalidationCoordinator;

    public Optional<FunctionPointAnalysis> findByProjectId(Long projectId) {
        return functionPointAnalysisRepository.findByEstimationProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public Optional<FunctionPointAnalysis> findDetailedByProjectId(Long projectId) {
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisRepository.findByEstimationProjectId(projectId);

        optionalAnalysis.ifPresent(this::initializeAnalysisCollections);
        return optionalAnalysis;
    }

    @Transactional
    public void createInitialAnalysis(EstimationProject estimationProject, String systemBoundaryDescription) {
        FunctionPointAnalysis analysis =
                new FunctionPointAnalysis(estimationProject, normalizeText(systemBoundaryDescription));

        initializeGeneralSystemCharacteristics(analysis);
        initializeDefaultWeightMatrix(analysis);
        functionPointCalculationService.recalculateAnalysis(analysis);
        functionPointAnalysisRepository.save(analysis);;
    }

    @Transactional
    public boolean updateSystemBoundaryDescription(Long projectId, String systemBoundaryDescription) {
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisRepository.findByEstimationProjectId(projectId);

        if (optionalAnalysis.isEmpty()) {
            return false;
        }

        String normalizedBoundary = normalizeText(systemBoundaryDescription);

        if (normalizedBoundary == null || normalizedBoundary.isBlank()) {
            return false;
        }

        FunctionPointAnalysis analysis = optionalAnalysis.get();
        analysis.setSystemBoundaryDescription(normalizedBoundary);

        recalculateManagedAnalysis(analysis);
        return true;
    }

    @Transactional
    public boolean updateGeneralSystemCharacteristics(Long projectId, FunctionPointAnalysis formAnalysis) {
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisRepository.findByEstimationProjectId(projectId);

        if (optionalAnalysis.isEmpty()) {
            return false;
        }

        FunctionPointAnalysis analysis = optionalAnalysis.get();

        for (GeneralSystemCharacteristicAssessment existing : analysis.getGeneralSystemCharacteristicAssessments()) {
            for (GeneralSystemCharacteristicAssessment incoming : formAnalysis.getGeneralSystemCharacteristicAssessments()) {
                if (existing.getCharacteristicType() == incoming.getCharacteristicType()) {
                    existing.setDegreeOfInfluence(normalizeDegreeOfInfluence(incoming.getDegreeOfInfluence()));
                    break;
                }
            }
        }

        recalculateManagedAnalysis(analysis);
        return true;
    }

    @Transactional
    public void deleteByProjectId(Long projectId) {
        functionPointAnalysisRepository.findByEstimationProjectId(projectId)
                .ifPresent(functionPointAnalysisRepository::delete);
    }

    public Page<DataFunction> findDataFunctionsPageByProjectId(Long projectId, Pageable pageable) {
        Page<DataFunction> page =
                dataFunctionRepository.findByFunctionPointAnalysisEstimationProjectIdOrderByIdAsc(projectId, pageable);

        page.getContent().forEach(this::initializeDataFunctionReferences);
        return page;
    }

    public Page<TransactionalFunction> findTransactionalFunctionsPageByProjectId(Long projectId, Pageable pageable) {
        Page<TransactionalFunction> page =
                transactionalFunctionRepository.findByFunctionPointAnalysisEstimationProjectIdOrderByIdAsc(projectId, pageable);

        page.getContent().forEach(this::initializeTransactionalFunctionReferences);
        return page;
    }

    @Transactional(readOnly = true)
    public Page<DataFunction> findDataFunctionsPageByRequirementId(Long requirementId, Pageable pageable) {
        Page<DataFunction> page =
                dataFunctionRepository.findByUserRequirementIdOrderByIdAsc(requirementId, pageable);

        page.getContent().forEach(this::initializeDataFunctionReferences);
        return page;
    }

    @Transactional(readOnly = true)
    public Page<TransactionalFunction> findTransactionalFunctionsPageByRequirementId(Long requirementId, Pageable pageable) {
        Page<TransactionalFunction> page =
                transactionalFunctionRepository.findByUserRequirementIdOrderByIdAsc(requirementId, pageable);

        page.getContent().forEach(this::initializeTransactionalFunctionReferences);
        return page;
    }

    @Transactional
    public boolean addDataFunctionToRequirement(Long projectId, Long requirementId, DataFunction dataFunction) {
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisRepository.findByEstimationProjectId(projectId);
        Optional<UserRequirement> optionalRequirement =
                userRequirementRepository.findByIdAndFunctionPointModuleFunctionPointAnalysisEstimationProjectId(requirementId, projectId);

        if (optionalAnalysis.isEmpty() || optionalRequirement.isEmpty()) {
            return false;
        }

        FunctionPointAnalysis analysis = optionalAnalysis.get();
        UserRequirement requirement = optionalRequirement.get();

        dataFunction.setName(normalizeText(dataFunction.getName()));
        dataFunction.setDescription(normalizeText(dataFunction.getDescription()));
        dataFunction.setFunctionPointAnalysis(analysis);
        dataFunction.setUserRequirement(requirement);

        analysis.getDataFunctions().add(dataFunction);

        recalculateManagedAnalysis(analysis);
        return true;
    }

    @Transactional
    public boolean addTransactionalFunctionToRequirement(Long projectId,
                                                         Long requirementId,
                                                         TransactionalFunction transactionalFunction) {
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisRepository.findByEstimationProjectId(projectId);
        Optional<UserRequirement> optionalRequirement =
                userRequirementRepository.findByIdAndFunctionPointModuleFunctionPointAnalysisEstimationProjectId(requirementId, projectId);

        if (optionalAnalysis.isEmpty() || optionalRequirement.isEmpty()) {
            return false;
        }

        FunctionPointAnalysis analysis = optionalAnalysis.get();
        UserRequirement requirement = optionalRequirement.get();

        transactionalFunction.setName(normalizeText(transactionalFunction.getName()));
        transactionalFunction.setDescription(normalizeText(transactionalFunction.getDescription()));
        transactionalFunction.setFunctionPointAnalysis(analysis);
        transactionalFunction.setUserRequirement(requirement);

        analysis.getTransactionalFunctions().add(transactionalFunction);

        recalculateManagedAnalysis(analysis);
        return true;
    }

    @Transactional
    public boolean deleteDataFunction(Long projectId, Long dataFunctionId) {
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisRepository.findByEstimationProjectId(projectId);

        if (optionalAnalysis.isEmpty()) {
            return false;
        }

        FunctionPointAnalysis analysis = optionalAnalysis.get();
        Hibernate.initialize(analysis.getDataFunctions());

        boolean removed = analysis.getDataFunctions()
                .removeIf(dataFunction -> dataFunction.getId().equals(dataFunctionId));

        if (removed) {
            recalculateManagedAnalysis(analysis);
        }

        return removed;
    }

    @Transactional
    public boolean deleteTransactionalFunction(Long projectId, Long transactionalFunctionId) {
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisRepository.findByEstimationProjectId(projectId);

        if (optionalAnalysis.isEmpty()) {
            return false;
        }

        FunctionPointAnalysis analysis = optionalAnalysis.get();
        Hibernate.initialize(analysis.getTransactionalFunctions());

        boolean removed = analysis.getTransactionalFunctions()
                .removeIf(transactionalFunction -> transactionalFunction.getId().equals(transactionalFunctionId));

        if (removed) {
            recalculateManagedAnalysis(analysis);
        }

        return removed;
    }

    @Transactional(readOnly = true)
    public Optional<DataFunction> findDataFunction(Long projectId, Long dataFunctionId) {
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisRepository.findByEstimationProjectId(projectId);

        if (optionalAnalysis.isEmpty()) {
            return Optional.empty();
        }

        FunctionPointAnalysis analysis = optionalAnalysis.get();
        Hibernate.initialize(analysis.getDataFunctions());

        Optional<DataFunction> optionalDataFunction = analysis.getDataFunctions().stream()
                .filter(dataFunction -> dataFunction.getId().equals(dataFunctionId))
                .findFirst();

        optionalDataFunction.ifPresent(this::initializeDataFunctionReferences);
        return optionalDataFunction;
    }

    @Transactional(readOnly = true)
    public Optional<TransactionalFunction> findTransactionalFunction(Long projectId, Long transactionalFunctionId) {
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisRepository.findByEstimationProjectId(projectId);

        if (optionalAnalysis.isEmpty()) {
            return Optional.empty();
        }

        FunctionPointAnalysis analysis = optionalAnalysis.get();
        Hibernate.initialize(analysis.getTransactionalFunctions());

        Optional<TransactionalFunction> optionalTransactionalFunction =
                analysis.getTransactionalFunctions().stream()
                        .filter(transactionalFunction -> transactionalFunction.getId().equals(transactionalFunctionId))
                        .findFirst();

        optionalTransactionalFunction.ifPresent(this::initializeTransactionalFunctionReferences);
        return optionalTransactionalFunction;
    }

    @Transactional
    public boolean updateDataFunction(Long projectId, Long dataFunctionId, DataFunction formDataFunction) {
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisRepository.findByEstimationProjectId(projectId);

        if (optionalAnalysis.isEmpty()) {
            return false;
        }

        FunctionPointAnalysis analysis = optionalAnalysis.get();
        Hibernate.initialize(analysis.getDataFunctions());

        Optional<DataFunction> optionalDataFunction = analysis.getDataFunctions().stream()
                .filter(dataFunction -> dataFunction.getId().equals(dataFunctionId))
                .findFirst();

        if (optionalDataFunction.isEmpty()) {
            return false;
        }

        DataFunction existingDataFunction = optionalDataFunction.get();
        existingDataFunction.setType(formDataFunction.getType());
        existingDataFunction.setName(normalizeText(formDataFunction.getName()));
        existingDataFunction.setDescription(normalizeText(formDataFunction.getDescription()));
        existingDataFunction.setComplexity(formDataFunction.getComplexity());

        recalculateManagedAnalysis(analysis);
        return true;
    }

    @Transactional
    public boolean updateTransactionalFunction(Long projectId,
                                               Long transactionalFunctionId,
                                               TransactionalFunction formTransactionalFunction) {
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisRepository.findByEstimationProjectId(projectId);

        if (optionalAnalysis.isEmpty()) {
            return false;
        }

        FunctionPointAnalysis analysis = optionalAnalysis.get();
        Hibernate.initialize(analysis.getTransactionalFunctions());

        Optional<TransactionalFunction> optionalTransactionalFunction =
                analysis.getTransactionalFunctions().stream()
                        .filter(transactionalFunction -> transactionalFunction.getId().equals(transactionalFunctionId))
                        .findFirst();

        if (optionalTransactionalFunction.isEmpty()) {
            return false;
        }

        TransactionalFunction existingTransactionalFunction = optionalTransactionalFunction.get();
        existingTransactionalFunction.setType(formTransactionalFunction.getType());
        existingTransactionalFunction.setName(normalizeText(formTransactionalFunction.getName()));
        existingTransactionalFunction.setDescription(normalizeText(formTransactionalFunction.getDescription()));
        existingTransactionalFunction.setComplexity(formTransactionalFunction.getComplexity());

        recalculateManagedAnalysis(analysis);
        return true;
    }

    @Transactional
    public void recalculateAndDeleteDerivedEfforts(FunctionPointAnalysis analysis) {
        functionPointCalculationService.recalculateAnalysis(analysis);
        effortResultsInvalidationCoordinator.invalidateForSizeAnalysis(analysis);
    }


    public Page<DataFunction> findDataFunctionsPageByModuleId(Long moduleId, Pageable pageable) {
        Page<DataFunction> page =
                dataFunctionRepository.findByUserRequirementFunctionPointModuleIdOrderByIdAsc(moduleId, pageable);

        page.getContent().forEach(this::initializeDataFunctionReferences);
        return page;
    }

    public Page<TransactionalFunction> findTransactionalFunctionsPageByModuleId(Long moduleId, Pageable pageable) {
        Page<TransactionalFunction> page =
                transactionalFunctionRepository.findByUserRequirementFunctionPointModuleIdOrderByIdAsc(moduleId, pageable);

        page.getContent().forEach(this::initializeTransactionalFunctionReferences);
        return page;
    }

    public List<DataFunction> findAllDataFunctionsByModuleId(Long moduleId) {
        List<DataFunction> dataFunctions =
                dataFunctionRepository.findByUserRequirementFunctionPointModuleIdOrderByIdAsc(moduleId);

        dataFunctions.forEach(this::initializeDataFunctionReferences);
        return dataFunctions;
    }

    public List<TransactionalFunction> findAllTransactionalFunctionsByModuleId(Long moduleId) {
        List<TransactionalFunction> transactionalFunctions =
                transactionalFunctionRepository.findByUserRequirementFunctionPointModuleIdOrderByIdAsc(moduleId);

        transactionalFunctions.forEach(this::initializeTransactionalFunctionReferences);
        return transactionalFunctions;
    }

    @Transactional
    public Optional<FunctionPointWeightMatrixForm> buildWeightMatrixForm(Long projectId) {
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisRepository.findByEstimationProjectId(projectId);

        if (optionalAnalysis.isEmpty()) {
            return Optional.empty();
        }

        FunctionPointAnalysis analysis = optionalAnalysis.get();
        Hibernate.initialize(analysis.getWeightMatrixEntries());

        ensureDefaultWeightMatrix(analysis);
        sortWeightMatrixEntries(analysis);

        FunctionPointWeightMatrixForm form = new FunctionPointWeightMatrixForm();
        form.setRows(new ArrayList<>());

        for (FunctionPointFunctionType functionType : getOrderedFunctionTypes()) {
            FunctionPointWeightMatrixRowForm row = new FunctionPointWeightMatrixRowForm();
            row.setFunctionType(functionType);
            row.setLowWeight(resolveMatrixWeight(analysis, functionType, FunctionPointComplexity.LOW));
            row.setAverageWeight(resolveMatrixWeight(analysis, functionType, FunctionPointComplexity.AVERAGE));
            row.setHighWeight(resolveMatrixWeight(analysis, functionType, FunctionPointComplexity.HIGH));

            form.getRows().add(row);
        }

        return Optional.of(form);
    }

    @Transactional
    public boolean updateWeightMatrix(Long projectId, FunctionPointWeightMatrixForm form) {
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisRepository.findByEstimationProjectId(projectId);

        if (optionalAnalysis.isEmpty()) {
            return false;
        }

        FunctionPointAnalysis analysis = optionalAnalysis.get();
        Hibernate.initialize(analysis.getWeightMatrixEntries());

        ensureDefaultWeightMatrix(analysis);

        for (FunctionPointWeightMatrixRowForm row : form.getRows()) {
            if (row.getFunctionType() == null) {
                continue;
            }

            updateWeightMatrixEntry(
                    analysis,
                    row.getFunctionType(),
                    FunctionPointComplexity.LOW,
                    row.getLowWeight()
            );

            updateWeightMatrixEntry(
                    analysis,
                    row.getFunctionType(),
                    FunctionPointComplexity.AVERAGE,
                    row.getAverageWeight()
            );

            updateWeightMatrixEntry(
                    analysis,
                    row.getFunctionType(),
                    FunctionPointComplexity.HIGH,
                    row.getHighWeight()
            );
        }

        sortWeightMatrixEntries(analysis);
        recalculateManagedAnalysis(analysis);

        return true;
    }

    @Transactional
    public boolean resetWeightMatrixToDefault(Long projectId) {
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisRepository.findByEstimationProjectId(projectId);

        if (optionalAnalysis.isEmpty()) {
            return false;
        }

        FunctionPointAnalysis analysis = optionalAnalysis.get();
        Hibernate.initialize(analysis.getWeightMatrixEntries());

        ensureDefaultWeightMatrix(analysis);

        for (FunctionPointFunctionType functionType : getOrderedFunctionTypes()) {
            updateWeightMatrixEntry(
                    analysis,
                    functionType,
                    FunctionPointComplexity.LOW,
                    functionType.getDefaultWeight(FunctionPointComplexity.LOW)
            );

            updateWeightMatrixEntry(
                    analysis,
                    functionType,
                    FunctionPointComplexity.AVERAGE,
                    functionType.getDefaultWeight(FunctionPointComplexity.AVERAGE)
            );

            updateWeightMatrixEntry(
                    analysis,
                    functionType,
                    FunctionPointComplexity.HIGH,
                    functionType.getDefaultWeight(FunctionPointComplexity.HIGH)
            );
        }

        sortWeightMatrixEntries(analysis);
        recalculateManagedAnalysis(analysis);

        return true;
    }

    private void initializeAnalysisCollections(FunctionPointAnalysis analysis) {
        Hibernate.initialize(analysis.getDataFunctions());
        Hibernate.initialize(analysis.getTransactionalFunctions());
        Hibernate.initialize(analysis.getGeneralSystemCharacteristicAssessments());
        Hibernate.initialize(analysis.getWeightMatrixEntries());

        analysis.getGeneralSystemCharacteristicAssessments()
                .sort(Comparator.comparingInt(a -> a.getCharacteristicType().getOrder()));

        sortWeightMatrixEntries(analysis);

        analysis.getDataFunctions().forEach(this::initializeDataFunctionReferences);
        analysis.getTransactionalFunctions().forEach(this::initializeTransactionalFunctionReferences);
    }

    private void initializeDataFunctionReferences(DataFunction dataFunction) {
        if (dataFunction.getUserRequirement() != null) {
            Hibernate.initialize(dataFunction.getUserRequirement());

            if (dataFunction.getUserRequirement().getFunctionPointModule() != null) {
                Hibernate.initialize(dataFunction.getUserRequirement().getFunctionPointModule());
            }
        }
    }

    private void initializeTransactionalFunctionReferences(TransactionalFunction transactionalFunction) {
        if (transactionalFunction.getUserRequirement() != null) {
            Hibernate.initialize(transactionalFunction.getUserRequirement());

            if (transactionalFunction.getUserRequirement().getFunctionPointModule() != null) {
                Hibernate.initialize(transactionalFunction.getUserRequirement().getFunctionPointModule());
            }
        }
    }

    private void initializeGeneralSystemCharacteristics(FunctionPointAnalysis analysis) {
        for (GeneralSystemCharacteristicType type : GeneralSystemCharacteristicType.values()) {
            GeneralSystemCharacteristicAssessment assessment = new GeneralSystemCharacteristicAssessment();
            assessment.setFunctionPointAnalysis(analysis);
            assessment.setCharacteristicType(type);
            assessment.setDegreeOfInfluence(0);

            analysis.getGeneralSystemCharacteristicAssessments().add(assessment);
        }
    }

    private void initializeDefaultWeightMatrix(FunctionPointAnalysis analysis) {
        ensureDefaultWeightMatrix(analysis);
    }

    private void ensureDefaultWeightMatrix(FunctionPointAnalysis analysis) {
        for (FunctionPointFunctionType functionType : getOrderedFunctionTypes()) {
            ensureDefaultWeightEntry(analysis, functionType, FunctionPointComplexity.LOW);
            ensureDefaultWeightEntry(analysis, functionType, FunctionPointComplexity.AVERAGE);
            ensureDefaultWeightEntry(analysis, functionType, FunctionPointComplexity.HIGH);
        }

        sortWeightMatrixEntries(analysis);
    }

    private void ensureDefaultWeightEntry(FunctionPointAnalysis analysis,
                                          FunctionPointFunctionType functionType,
                                          FunctionPointComplexity complexity) {
        boolean exists = analysis.getWeightMatrixEntries()
                .stream()
                .anyMatch(entry ->
                        entry.getFunctionType() == functionType
                                && entry.getComplexity() == complexity
                );

        if (!exists) {
            addDefaultWeightEntry(analysis, functionType, complexity);
        }
    }

    private void addDefaultWeightEntry(FunctionPointAnalysis analysis,
                                       FunctionPointFunctionType functionType,
                                       FunctionPointComplexity complexity) {
        FunctionPointWeightMatrixEntry entry = new FunctionPointWeightMatrixEntry();
        entry.setFunctionType(functionType);
        entry.setComplexity(complexity);
        entry.setWeight(functionType.getDefaultWeight(complexity));
        entry.setDisplayOrder(functionType.getDisplayOrder());

        analysis.addWeightMatrixEntry(entry);
    }

    private void updateWeightMatrixEntry(FunctionPointAnalysis analysis,
                                         FunctionPointFunctionType functionType,
                                         FunctionPointComplexity complexity,
                                         Integer weight) {
        FunctionPointWeightMatrixEntry entry = analysis.getWeightMatrixEntries()
                .stream()
                .filter(existing -> existing.getFunctionType() == functionType)
                .filter(existing -> existing.getComplexity() == complexity)
                .findFirst()
                .orElseGet(() -> {
                    FunctionPointWeightMatrixEntry newEntry = new FunctionPointWeightMatrixEntry();
                    newEntry.setFunctionType(functionType);
                    newEntry.setComplexity(complexity);
                    newEntry.setDisplayOrder(functionType.getDisplayOrder());
                    analysis.addWeightMatrixEntry(newEntry);
                    return newEntry;
                });

        entry.setWeight(normalizeFunctionWeight(weight));
    }

    private Integer resolveMatrixWeight(FunctionPointAnalysis analysis,
                                        FunctionPointFunctionType functionType,
                                        FunctionPointComplexity complexity) {
        return analysis.getWeightMatrixEntries()
                .stream()
                .filter(entry -> entry.getFunctionType() == functionType)
                .filter(entry -> entry.getComplexity() == complexity)
                .findFirst()
                .map(FunctionPointWeightMatrixEntry::getWeight)
                .orElse(functionType.getDefaultWeight(complexity));
    }

    private Integer normalizeFunctionWeight(Integer value) {
        if (value == null || value < 1) {
            return 1;
        }

        return Math.min(value, 999);
    }

    private List<FunctionPointFunctionType> getOrderedFunctionTypes() {
        return List.of(
                FunctionPointFunctionType.EI,
                FunctionPointFunctionType.EO,
                FunctionPointFunctionType.EQ,
                FunctionPointFunctionType.ILF,
                FunctionPointFunctionType.EIF
        );
    }

    private void sortWeightMatrixEntries(FunctionPointAnalysis analysis) {
        analysis.getWeightMatrixEntries()
                .sort(
                        Comparator
                                .comparingInt(FunctionPointWeightMatrixEntry::getDisplayOrder)
                                .thenComparing(entry -> entry.getComplexity().ordinal())
                );
    }

    private Integer normalizeDegreeOfInfluence(Integer value) {
        if (value == null || value < 0) {
            return 0;
        }
        return Math.min(value, 5);
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private void recalculateManagedAnalysis(FunctionPointAnalysis analysis) {
        recalculateAndDeleteDerivedEfforts(analysis);
    }
}