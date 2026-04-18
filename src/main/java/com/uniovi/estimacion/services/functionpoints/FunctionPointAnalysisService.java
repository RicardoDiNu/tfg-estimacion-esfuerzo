package com.uniovi.estimacion.services.functionpoints;

import com.uniovi.estimacion.entities.EstimationProject;
import com.uniovi.estimacion.entities.functionpoints.DataFunction;
import com.uniovi.estimacion.entities.functionpoints.FunctionPointAnalysis;
import com.uniovi.estimacion.entities.functionpoints.GeneralSystemCharacteristicAssessment;
import com.uniovi.estimacion.entities.functionpoints.GeneralSystemCharacteristicType;
import com.uniovi.estimacion.entities.functionpoints.TransactionalFunction;
import com.uniovi.estimacion.repositories.DataFunctionRepository;
import com.uniovi.estimacion.repositories.FunctionPointAnalysisRepository;
import com.uniovi.estimacion.repositories.TransactionalFunctionRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;


import com.uniovi.estimacion.entities.requirements.UserRequirement;
import com.uniovi.estimacion.repositories.UserRequirementRepository;

@Service
@RequiredArgsConstructor
public class FunctionPointAnalysisService {

    private final FunctionPointAnalysisRepository functionPointAnalysisRepository;
    private final DataFunctionRepository dataFunctionRepository;
    private final TransactionalFunctionRepository transactionalFunctionRepository;
    private final FunctionPointCalculationService functionPointCalculationService;
    private final UserRequirementRepository userRequirementRepository;

    public Optional<FunctionPointAnalysis> getByProjectId(Long projectId) {
        return functionPointAnalysisRepository.findByEstimationProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public Optional<FunctionPointAnalysis> getDetailedByProjectId(Long projectId) {
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisRepository.findByEstimationProjectId(projectId);

        optionalAnalysis.ifPresent(analysis -> {
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
        });

        return optionalAnalysis;
    }

    public Optional<FunctionPointAnalysis> getById(Long id) {
        return functionPointAnalysisRepository.findById(id);
    }

    public void save(FunctionPointAnalysis analysis) {
        functionPointCalculationService.recalculateAnalysis(analysis);
        functionPointAnalysisRepository.save(analysis);
    }

    public FunctionPointAnalysis createInitialAnalysis(EstimationProject estimationProject,
                                                       String systemBoundaryDescription) {
        FunctionPointAnalysis analysis = new FunctionPointAnalysis(estimationProject, systemBoundaryDescription);

        initializeGeneralSystemCharacteristics(analysis);
        functionPointCalculationService.recalculateAnalysis(analysis);

        return functionPointAnalysisRepository.save(analysis);
    }

    @Transactional
    public boolean addDataFunctionToProject(Long projectId, DataFunction dataFunction) {
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisRepository.findByEstimationProjectId(projectId);

        if (optionalAnalysis.isEmpty()) {
            return false;
        }

        FunctionPointAnalysis analysis = optionalAnalysis.get();

        dataFunction.setFunctionPointAnalysis(analysis);
        analysis.getDataFunctions().add(dataFunction);

        functionPointCalculationService.recalculateAnalysis(analysis);
        functionPointAnalysisRepository.save(analysis);

        return true;
    }

    @Transactional
    public boolean addTransactionalFunctionToProject(Long projectId, TransactionalFunction transactionalFunction) {
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisRepository.findByEstimationProjectId(projectId);

        if (optionalAnalysis.isEmpty()) {
            return false;
        }

        FunctionPointAnalysis analysis = optionalAnalysis.get();

        transactionalFunction.setFunctionPointAnalysis(analysis);
        analysis.getTransactionalFunctions().add(transactionalFunction);

        functionPointCalculationService.recalculateAnalysis(analysis);
        functionPointAnalysisRepository.save(analysis);

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
                if (existing.getId().equals(incoming.getId())) {
                    int value = incoming.getDegreeOfInfluence() == null ? 0 : incoming.getDegreeOfInfluence();

                    if (value < 0) {
                        value = 0;
                    }
                    if (value > 5) {
                        value = 5;
                    }

                    existing.setDegreeOfInfluence(value);
                    break;
                }
            }
        }

        functionPointCalculationService.recalculateAnalysis(analysis);
        functionPointAnalysisRepository.save(analysis);

        return true;
    }

    @Transactional
    public boolean deleteDataFunctionFromProject(Long projectId, Long dataFunctionId) {
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisRepository.findByEstimationProjectId(projectId);
        Optional<DataFunction> optionalDataFunction = dataFunctionRepository.findById(dataFunctionId);

        if (optionalAnalysis.isEmpty() || optionalDataFunction.isEmpty()) {
            return false;
        }

        FunctionPointAnalysis analysis = optionalAnalysis.get();
        DataFunction dataFunction = optionalDataFunction.get();

        if (!dataFunction.getFunctionPointAnalysis().getId().equals(analysis.getId())) {
            return false;
        }

        analysis.getDataFunctions().removeIf(df -> df.getId().equals(dataFunctionId));

        functionPointCalculationService.recalculateAnalysis(analysis);
        functionPointAnalysisRepository.save(analysis);

        return true;
    }

    @Transactional
    public boolean deleteTransactionalFunctionFromProject(Long projectId, Long transactionalFunctionId) {
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisRepository.findByEstimationProjectId(projectId);
        Optional<TransactionalFunction> optionalTransactionalFunction =
                transactionalFunctionRepository.findById(transactionalFunctionId);

        if (optionalAnalysis.isEmpty() || optionalTransactionalFunction.isEmpty()) {
            return false;
        }

        FunctionPointAnalysis analysis = optionalAnalysis.get();
        TransactionalFunction transactionalFunction = optionalTransactionalFunction.get();

        if (!transactionalFunction.getFunctionPointAnalysis().getId().equals(analysis.getId())) {
            return false;
        }

        analysis.getTransactionalFunctions().removeIf(tf -> tf.getId().equals(transactionalFunctionId));

        functionPointCalculationService.recalculateAnalysis(analysis);
        functionPointAnalysisRepository.save(analysis);

        return true;
    }

    public void deleteByProjectId(Long projectId) {
        functionPointAnalysisRepository.findByEstimationProjectId(projectId)
                .ifPresent(functionPointAnalysisRepository::delete);
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

    @Transactional
    public boolean updateDataFunctionInProject(Long projectId, DataFunction formDataFunction) {
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisRepository.findByEstimationProjectId(projectId);
        Optional<DataFunction> optionalDataFunction =
                dataFunctionRepository.findById(formDataFunction.getId());

        if (optionalAnalysis.isEmpty() || optionalDataFunction.isEmpty()) {
            return false;
        }

        FunctionPointAnalysis analysis = optionalAnalysis.get();
        DataFunction existingDataFunction = optionalDataFunction.get();

        if (!existingDataFunction.getFunctionPointAnalysis().getId().equals(analysis.getId())) {
            return false;
        }

        existingDataFunction.setType(formDataFunction.getType());
        existingDataFunction.setName(formDataFunction.getName());
        existingDataFunction.setDescription(formDataFunction.getDescription());
        existingDataFunction.setDetCount(formDataFunction.getDetCount());
        existingDataFunction.setRetCount(formDataFunction.getRetCount());

        functionPointCalculationService.recalculateAnalysis(analysis);
        functionPointAnalysisRepository.save(analysis);

        return true;
    }

    @Transactional
    public boolean updateTransactionalFunctionInProject(Long projectId,
                                                        TransactionalFunction formTransactionalFunction) {
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisRepository.findByEstimationProjectId(projectId);
        Optional<TransactionalFunction> optionalTransactionalFunction =
                transactionalFunctionRepository.findById(formTransactionalFunction.getId());

        if (optionalAnalysis.isEmpty() || optionalTransactionalFunction.isEmpty()) {
            return false;
        }

        FunctionPointAnalysis analysis = optionalAnalysis.get();
        TransactionalFunction existingTransactionalFunction = optionalTransactionalFunction.get();

        if (!existingTransactionalFunction.getFunctionPointAnalysis().getId().equals(analysis.getId())) {
            return false;
        }

        existingTransactionalFunction.setType(formTransactionalFunction.getType());
        existingTransactionalFunction.setName(formTransactionalFunction.getName());
        existingTransactionalFunction.setDescription(formTransactionalFunction.getDescription());
        existingTransactionalFunction.setDetCount(formTransactionalFunction.getDetCount());
        existingTransactionalFunction.setFtrCount(formTransactionalFunction.getFtrCount());

        functionPointCalculationService.recalculateAnalysis(analysis);
        functionPointAnalysisRepository.save(analysis);

        return true;
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

        functionPointCalculationService.recalculateAnalysis(analysis);
        functionPointAnalysisRepository.save(analysis);

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

        functionPointCalculationService.recalculateAnalysis(analysis);
        functionPointAnalysisRepository.save(analysis);

        return true;
    }

    @Transactional
    public boolean deleteDataFunctionInProject(Long projectId, Long dataFunctionId) {
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisRepository.findByEstimationProjectId(projectId);

        if (optionalAnalysis.isEmpty()) {
            return false;
        }

        FunctionPointAnalysis analysis = optionalAnalysis.get();
        Hibernate.initialize(analysis.getDataFunctions());

        boolean removed = analysis.getDataFunctions().removeIf(df -> df.getId().equals(dataFunctionId));

        if (removed) {
            functionPointCalculationService.recalculateAnalysis(analysis);
            functionPointAnalysisRepository.save(analysis);
        }

        return removed;
    }

    @Transactional
    public boolean deleteTransactionalFunctionInProject(Long projectId, Long transactionalFunctionId) {
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisRepository.findByEstimationProjectId(projectId);

        if (optionalAnalysis.isEmpty()) {
            return false;
        }

        FunctionPointAnalysis analysis = optionalAnalysis.get();
        Hibernate.initialize(analysis.getTransactionalFunctions());

        boolean removed = analysis.getTransactionalFunctions()
                .removeIf(tf -> tf.getId().equals(transactionalFunctionId));

        if (removed) {
            functionPointCalculationService.recalculateAnalysis(analysis);
            functionPointAnalysisRepository.save(analysis);
        }

        return removed;
    }

    @Transactional(readOnly = true)
    public Optional<DataFunction> getDataFunctionInProject(Long projectId, Long dataFunctionId) {
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisRepository.findByEstimationProjectId(projectId);

        if (optionalAnalysis.isEmpty()) {
            return Optional.empty();
        }

        FunctionPointAnalysis analysis = optionalAnalysis.get();
        Hibernate.initialize(analysis.getDataFunctions());

        return analysis.getDataFunctions().stream()
                .filter(df -> df.getId().equals(dataFunctionId))
                .findFirst();
    }

    @Transactional(readOnly = true)
    public Optional<TransactionalFunction> getTransactionalFunctionInProject(Long projectId,
                                                                             Long transactionalFunctionId) {
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisRepository.findByEstimationProjectId(projectId);

        if (optionalAnalysis.isEmpty()) {
            return Optional.empty();
        }

        FunctionPointAnalysis analysis = optionalAnalysis.get();
        Hibernate.initialize(analysis.getTransactionalFunctions());

        return analysis.getTransactionalFunctions().stream()
                .filter(tf -> tf.getId().equals(transactionalFunctionId))
                .findFirst();
    }

    @Transactional
    public boolean updateDataFunctionInProject(Long projectId, Long dataFunctionId, DataFunction formDataFunction) {
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisRepository.findByEstimationProjectId(projectId);

        if (optionalAnalysis.isEmpty()) {
            return false;
        }

        FunctionPointAnalysis analysis = optionalAnalysis.get();
        Hibernate.initialize(analysis.getDataFunctions());

        Optional<DataFunction> optionalDataFunction = analysis.getDataFunctions().stream()
                .filter(df -> df.getId().equals(dataFunctionId))
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

        functionPointCalculationService.recalculateAnalysis(analysis);
        functionPointAnalysisRepository.save(analysis);

        return true;
    }

    @Transactional
    public boolean updateTransactionalFunctionInProject(Long projectId,
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
                        .filter(tf -> tf.getId().equals(transactionalFunctionId))
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

        functionPointCalculationService.recalculateAnalysis(analysis);
        functionPointAnalysisRepository.save(analysis);

        return true;
    }

}