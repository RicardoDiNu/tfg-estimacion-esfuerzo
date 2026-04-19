package com.uniovi.estimacion.services.functionpoints;

import com.uniovi.estimacion.entities.functionpoints.DataFunction;
import com.uniovi.estimacion.entities.functionpoints.FunctionPointAnalysis;
import com.uniovi.estimacion.entities.functionpoints.GeneralSystemCharacteristicAssessment;
import com.uniovi.estimacion.entities.functionpoints.GeneralSystemCharacteristicType;
import com.uniovi.estimacion.entities.functionpoints.TransactionalFunction;
import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.requirements.UserRequirement;
import com.uniovi.estimacion.repositories.functionpoints.DataFunctionRepository;
import com.uniovi.estimacion.repositories.functionpoints.FunctionPointAnalysisRepository;
import com.uniovi.estimacion.repositories.functionpoints.TransactionalFunctionRepository;
import com.uniovi.estimacion.repositories.requirements.UserRequirementRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        FunctionPointAnalysis analysis = new FunctionPointAnalysis(estimationProject, systemBoundaryDescription);
        initializeGeneralSystemCharacteristics(analysis);
        functionPointCalculationService.recalculateAnalysis(analysis);
        functionPointAnalysisRepository.save(analysis);
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

        recalculateAndSave(analysis);
        return true;
    }

    @Transactional
    public void deleteByProjectId(Long projectId) {
        functionPointAnalysisRepository.findByEstimationProjectId(projectId)
                .ifPresent(functionPointAnalysisRepository::delete);
    }

    public Page<DataFunction> findDataFunctionsPageByProjectId(Long projectId, Pageable pageable) {
        return dataFunctionRepository.findByFunctionPointAnalysisEstimationProjectIdOrderByIdAsc(projectId, pageable);
    }

    public Page<TransactionalFunction> findTransactionalFunctionsPageByProjectId(Long projectId, Pageable pageable) {
        return transactionalFunctionRepository.findByFunctionPointAnalysisEstimationProjectIdOrderByIdAsc(projectId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<DataFunction> findDataFunctionsPageByRequirementId(Long requirementId, Pageable pageable) {
        return dataFunctionRepository.findByUserRequirementIdOrderByIdAsc(requirementId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<TransactionalFunction> findTransactionalFunctionsPageByRequirementId(Long requirementId, Pageable pageable) {
        return transactionalFunctionRepository.findByUserRequirementIdOrderByIdAsc(requirementId, pageable);
    }

    @Transactional
    public boolean addDataFunctionToRequirement(Long projectId, Long requirementId, DataFunction dataFunction) {
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisRepository.findByEstimationProjectId(projectId);
        Optional<UserRequirement> optionalRequirement =
                userRequirementRepository.findByIdAndEstimationProjectId(requirementId, projectId);

        if (optionalAnalysis.isEmpty() || optionalRequirement.isEmpty()) {
            return false;
        }

        FunctionPointAnalysis analysis = optionalAnalysis.get();
        UserRequirement requirement = optionalRequirement.get();

        dataFunction.setFunctionPointAnalysis(analysis);
        dataFunction.setUserRequirement(requirement);

        analysis.getDataFunctions().add(dataFunction);
        requirement.getDataFunctions().add(dataFunction);

        recalculateAndSave(analysis);
        return true;
    }

    @Transactional
    public boolean addTransactionalFunctionToRequirement(Long projectId,
                                                         Long requirementId,
                                                         TransactionalFunction transactionalFunction) {
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisRepository.findByEstimationProjectId(projectId);
        Optional<UserRequirement> optionalRequirement =
                userRequirementRepository.findByIdAndEstimationProjectId(requirementId, projectId);

        if (optionalAnalysis.isEmpty() || optionalRequirement.isEmpty()) {
            return false;
        }

        FunctionPointAnalysis analysis = optionalAnalysis.get();
        UserRequirement requirement = optionalRequirement.get();

        transactionalFunction.setFunctionPointAnalysis(analysis);
        transactionalFunction.setUserRequirement(requirement);

        analysis.getTransactionalFunctions().add(transactionalFunction);
        requirement.getTransactionalFunctions().add(transactionalFunction);

        recalculateAndSave(analysis);
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

        boolean removed = analysis.getDataFunctions().removeIf(dataFunction -> dataFunction.getId().equals(dataFunctionId));

        if (removed) {
            recalculateAndSave(analysis);
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
            recalculateAndSave(analysis);
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

        return analysis.getDataFunctions().stream()
                .filter(dataFunction -> dataFunction.getId().equals(dataFunctionId))
                .findFirst();
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

        return analysis.getTransactionalFunctions().stream()
                .filter(transactionalFunction -> transactionalFunction.getId().equals(transactionalFunctionId))
                .findFirst();
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
        existingDataFunction.setName(formDataFunction.getName());
        existingDataFunction.setDescription(formDataFunction.getDescription());
        existingDataFunction.setDetCount(formDataFunction.getDetCount());
        existingDataFunction.setRetCount(formDataFunction.getRetCount());

        recalculateAndSave(analysis);
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
        existingTransactionalFunction.setName(formTransactionalFunction.getName());
        existingTransactionalFunction.setDescription(formTransactionalFunction.getDescription());
        existingTransactionalFunction.setDetCount(formTransactionalFunction.getDetCount());
        existingTransactionalFunction.setFtrCount(formTransactionalFunction.getFtrCount());

        recalculateAndSave(analysis);
        return true;
    }

    private void initializeAnalysisCollections(FunctionPointAnalysis analysis) {
        Hibernate.initialize(analysis.getDataFunctions());
        Hibernate.initialize(analysis.getTransactionalFunctions());
        Hibernate.initialize(analysis.getGeneralSystemCharacteristicAssessments());

        analysis.getDataFunctions().forEach(dataFunction -> {
            if (dataFunction.getUserRequirement() != null) {
                Hibernate.initialize(dataFunction.getUserRequirement());
            }
        });

        analysis.getTransactionalFunctions().forEach(transactionalFunction -> {
            if (transactionalFunction.getUserRequirement() != null) {
                Hibernate.initialize(transactionalFunction.getUserRequirement());
            }
        });
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

    private void recalculateAndSave(FunctionPointAnalysis analysis) {
        functionPointCalculationService.recalculateAnalysis(analysis);
        functionPointAnalysisRepository.save(analysis);
    }
}