package com.uniovi.estimacion.services.functionpoints;

import com.uniovi.estimacion.entities.functionpoints.FunctionPointAnalysis;
import com.uniovi.estimacion.entities.functionpoints.functions.DataFunction;
import com.uniovi.estimacion.entities.functionpoints.functions.TransactionalFunction;
import com.uniovi.estimacion.entities.functionpoints.gscs.GeneralSystemCharacteristicAssessment;
import com.uniovi.estimacion.entities.functionpoints.gscs.GeneralSystemCharacteristicType;
import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.requirements.UserRequirement;
import com.uniovi.estimacion.repositories.functionpoints.DataFunctionRepository;
import com.uniovi.estimacion.repositories.functionpoints.FunctionPointAnalysisRepository;
import com.uniovi.estimacion.repositories.functionpoints.TransactionalFunctionRepository;
import com.uniovi.estimacion.repositories.requirements.UserRequirementRepository;
import com.uniovi.estimacion.entities.projects.EstimationModule;
import com.uniovi.estimacion.repositories.projects.EstimationModuleRepository;
import com.uniovi.estimacion.services.effortconversions.EffortResultsInvalidationCoordinator;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final EstimationModuleRepository estimationModuleRepository;
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
        functionPointCalculationService.recalculateAnalysis(analysis);
        functionPointAnalysisRepository.save(analysis);
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
                userRequirementRepository.findByIdAndEstimationModuleEstimationProjectId(requirementId, projectId);

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
                userRequirementRepository.findByIdAndEstimationModuleEstimationProjectId(requirementId, projectId);

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
        effortResultsInvalidationCoordinator.deleteForFunctionPointAnalysis(analysis);
    }


    public Page<DataFunction> findDataFunctionsPageByModuleId(Long moduleId, Pageable pageable) {
        Page<DataFunction> page =
                dataFunctionRepository.findByUserRequirementEstimationModuleIdOrderByIdAsc(moduleId, pageable);

        page.getContent().forEach(this::initializeDataFunctionReferences);
        return page;
    }

    public Page<TransactionalFunction> findTransactionalFunctionsPageByModuleId(Long moduleId, Pageable pageable) {
        Page<TransactionalFunction> page =
                transactionalFunctionRepository.findByUserRequirementEstimationModuleIdOrderByIdAsc(moduleId, pageable);

        page.getContent().forEach(this::initializeTransactionalFunctionReferences);
        return page;
    }

    public List<DataFunction> findAllDataFunctionsByModuleId(Long moduleId) {
        List<DataFunction> dataFunctions =
                dataFunctionRepository.findByUserRequirementEstimationModuleIdOrderByIdAsc(moduleId);

        dataFunctions.forEach(this::initializeDataFunctionReferences);
        return dataFunctions;
    }

    public List<TransactionalFunction> findAllTransactionalFunctionsByModuleId(Long moduleId) {
        List<TransactionalFunction> transactionalFunctions =
                transactionalFunctionRepository.findByUserRequirementEstimationModuleIdOrderByIdAsc(moduleId);

        transactionalFunctions.forEach(this::initializeTransactionalFunctionReferences);
        return transactionalFunctions;
    }

    private void initializeAnalysisCollections(FunctionPointAnalysis analysis) {
        Hibernate.initialize(analysis.getDataFunctions());
        Hibernate.initialize(analysis.getTransactionalFunctions());
        Hibernate.initialize(analysis.getGeneralSystemCharacteristicAssessments());

        analysis.getGeneralSystemCharacteristicAssessments()
                .sort(java.util.Comparator.comparingInt(a -> a.getCharacteristicType().getOrder()));

        analysis.getDataFunctions().forEach(this::initializeDataFunctionReferences);
        analysis.getTransactionalFunctions().forEach(this::initializeTransactionalFunctionReferences);
    }

    private void initializeDataFunctionReferences(DataFunction dataFunction) {
        if (dataFunction.getUserRequirement() != null) {
            Hibernate.initialize(dataFunction.getUserRequirement());

            if (dataFunction.getUserRequirement().getEstimationModule() != null) {
                Hibernate.initialize(dataFunction.getUserRequirement().getEstimationModule());
            }
        }
    }

    private void initializeTransactionalFunctionReferences(TransactionalFunction transactionalFunction) {
        if (transactionalFunction.getUserRequirement() != null) {
            Hibernate.initialize(transactionalFunction.getUserRequirement());

            if (transactionalFunction.getUserRequirement().getEstimationModule() != null) {
                Hibernate.initialize(transactionalFunction.getUserRequirement().getEstimationModule());
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