package com.uniovi.estimacion.services.effortconversions;

import com.uniovi.estimacion.entities.analysis.SizeAnalysis;
import com.uniovi.estimacion.entities.effortconversions.DelphiEstimation;
import com.uniovi.estimacion.entities.effortconversions.DelphiExpertEstimate;
import com.uniovi.estimacion.entities.effortconversions.DelphiIteration;
import com.uniovi.estimacion.entities.functionpoints.FunctionPointAnalysis;
import com.uniovi.estimacion.entities.functionpoints.functions.DataFunction;
import com.uniovi.estimacion.entities.functionpoints.functions.TransactionalFunction;
import com.uniovi.estimacion.entities.projects.EstimationModule;
import com.uniovi.estimacion.repositories.effortconversions.DelphiEstimationRepository;
import com.uniovi.estimacion.services.functionpoints.FunctionPointAnalysisService;
import com.uniovi.estimacion.services.functionpoints.FunctionPointAnalysisSummary;
import com.uniovi.estimacion.services.functionpoints.FunctionPointCalculationService;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DelphiEstimationService {

    private final DelphiEstimationRepository delphiEstimationRepository;

    public Optional<DelphiEstimation> findActiveBySourceAnalysis(SizeAnalysis sourceAnalysis) {
        return delphiEstimationRepository
                .findFirstBySourceAnalysisIdAndSourceTechniqueCodeAndActiveTrueOrderByCreatedAtDesc(
                        sourceAnalysis.getId(),
                        sourceAnalysis.getTechniqueCode()
                );
    }

    public List<DelphiEstimation> findHistoryBySourceAnalysis(SizeAnalysis sourceAnalysis) {
        return delphiEstimationRepository
                .findBySourceAnalysisIdAndSourceTechniqueCodeOrderByCreatedAtDesc(
                        sourceAnalysis.getId(),
                        sourceAnalysis.getTechniqueCode()
                );
    }

    public List<DelphiEstimation> findByProjectId(Long projectId) {
        return delphiEstimationRepository.findByEstimationProjectIdOrderByCreatedAtDesc(projectId);
    }

    @Transactional(readOnly = true)
    public Map<Long, Double> buildModuleSizeById(SizeAnalysis sourceAnalysis,
                                                 List<EstimationModule> modulesList,
                                                 FunctionPointAnalysisService functionPointAnalysisService,
                                                 FunctionPointCalculationService functionPointCalculationService) {
        if (!(sourceAnalysis instanceof FunctionPointAnalysis analysis)) {
            throw new IllegalArgumentException("UNSUPPORTED_SIZE_ANALYSIS_TYPE");
        }

        Map<Long, Double> moduleSizeById = new LinkedHashMap<>();

        for (EstimationModule module : modulesList) {
            List<DataFunction> moduleDataFunctions =
                    functionPointAnalysisService.findAllDataFunctionsByModuleId(module.getId());

            List<TransactionalFunction> moduleTransactionalFunctions =
                    functionPointAnalysisService.findAllTransactionalFunctionsByModuleId(module.getId());

            FunctionPointAnalysisSummary moduleSummary =
                    functionPointCalculationService.buildModuleSummary(
                            analysis,
                            moduleDataFunctions,
                            moduleTransactionalFunctions
                    );

            moduleSizeById.put(module.getId(), moduleSummary.getAdjustedFunctionPoints());
        }

        return moduleSizeById;
    }

    @Transactional(readOnly = true)
    public boolean canStartCalibration(Map<Long, Double> moduleSizeById) {
        return moduleSizeById.values().stream()
                .filter(size -> size != null && size > 0)
                .distinct()
                .count() >= 2;
    }

    @Transactional(readOnly = true)
    public boolean isFinished(DelphiEstimation estimation) {
        return estimation.getRegressionIntercept() != null
                && estimation.getRegressionSlope() != null;
    }

    @Transactional(readOnly = true)
    public int countIterations(DelphiEstimation estimation) {
        if (estimation.getIterations() == null) {
            return 0;
        }
        return estimation.getIterations().size();
    }

    @Transactional
    public DelphiEstimation createInitialEstimation(SizeAnalysis sourceAnalysis,
                                                    List<EstimationModule> projectModules,
                                                    Map<Long, Double> moduleSizeById,
                                                    Double confidencePercentage,
                                                    Double acceptableDeviationPercentage,
                                                    Integer maximumIterations) {
        assertValidSourceAnalysis(sourceAnalysis);

        ModuleReference minimumModule = findMinimumModule(projectModules, moduleSizeById);
        ModuleReference maximumModule = findMaximumModule(projectModules, moduleSizeById);

        assertExtremeModules(minimumModule, maximumModule);

        deactivatePreviousActiveEstimations(sourceAnalysis);

        DelphiEstimation estimation = new DelphiEstimation();
        estimation.setEstimationProject(sourceAnalysis.getEstimationProject());

        estimation.setSourceAnalysisId(sourceAnalysis.getId());
        estimation.setSourceTechniqueCode(sourceAnalysis.getTechniqueCode());
        estimation.setSourceSizeUnitCode(sourceAnalysis.getSizeUnitCode());
        estimation.setSourceProjectSizeSnapshot(sourceAnalysis.getCalculatedSizeValue());

        estimation.setMinimumModuleId(minimumModule.moduleId());
        estimation.setMinimumModuleNameSnapshot(minimumModule.moduleName());
        estimation.setMinimumModuleSizeSnapshot(minimumModule.moduleSize());

        estimation.setMaximumModuleId(maximumModule.moduleId());
        estimation.setMaximumModuleNameSnapshot(maximumModule.moduleName());
        estimation.setMaximumModuleSizeSnapshot(maximumModule.moduleSize());

        estimation.setConfidencePercentage(confidencePercentage != null ? confidencePercentage : 95.0);
        estimation.setAcceptableDeviationPercentage(
                acceptableDeviationPercentage != null ? acceptableDeviationPercentage : 10.0
        );
        estimation.setMaximumIterations(maximumIterations != null ? maximumIterations : 2);

        estimation.setActive(true);

        return delphiEstimationRepository.save(estimation);
    }

    @Transactional
    public DelphiEstimation applyFinalCalibration(DelphiEstimation estimation,
                                                  Double minimumModuleEstimatedEffortHours,
                                                  Double maximumModuleEstimatedEffortHours) {
        applyFinalCalibrationInternal(
                estimation,
                minimumModuleEstimatedEffortHours,
                maximumModuleEstimatedEffortHours
        );

        return delphiEstimationRepository.save(estimation);
    }

    public double calculateEstimatedEffortHours(DelphiEstimation estimation, Double moduleSize) {
        assertCalibrationAvailable(estimation);

        if (moduleSize == null || moduleSize < 0) {
            throw new IllegalArgumentException("INVALID_MODULE_SIZE");
        }

        return estimation.getRegressionIntercept() + (estimation.getRegressionSlope() * moduleSize);
    }

    public double calculateTotalEstimatedEffortHours(DelphiEstimation estimation,
                                                     Map<Long, Double> moduleSizeById) {
        assertCalibrationAvailable(estimation);

        return moduleSizeById.values().stream()
                .filter(size -> size != null && size >= 0)
                .mapToDouble(size -> calculateEstimatedEffortHours(estimation, size))
                .sum();
    }

    @Transactional
    public DelphiEstimation registerIteration(Long delphiEstimationId,
                                              List<DelphiExpertEstimate> expertEstimates) {
        DelphiEstimation estimation = delphiEstimationRepository.findById(delphiEstimationId)
                .orElseThrow(() -> new IllegalArgumentException("DELPHI_ESTIMATION_NOT_FOUND"));

        assertEstimationOpenForIteration(estimation);
        assertExpertEstimatesReady(expertEstimates);

        int nextIterationNumber = estimation.getIterations().size() + 1;

        DelphiIteration iteration = new DelphiIteration();
        iteration.setIterationNumber(nextIterationNumber);

        for (DelphiExpertEstimate incomingEstimate : expertEstimates) {
            DelphiExpertEstimate storedEstimate = new DelphiExpertEstimate();
            storedEstimate.setEvaluatorAlias(normalizeText(incomingEstimate.getEvaluatorAlias()));
            storedEstimate.setMinimumModuleEstimatedEffortHours(
                    incomingEstimate.getMinimumModuleEstimatedEffortHours()
            );
            storedEstimate.setMaximumModuleEstimatedEffortHours(
                    incomingEstimate.getMaximumModuleEstimatedEffortHours()
            );
            storedEstimate.setComments(normalizeText(incomingEstimate.getComments()));

            iteration.addExpertEstimate(storedEstimate);
        }

        double minimumDeviationPercentage =
                calculateDeviationPercentageForMinimumModule(iteration.getExpertEstimates());

        double maximumDeviationPercentage =
                calculateDeviationPercentageForMaximumModule(iteration.getExpertEstimates());

        boolean acceptedByDeviation =
                minimumDeviationPercentage <= estimation.getAcceptableDeviationPercentage()
                        && maximumDeviationPercentage <= estimation.getAcceptableDeviationPercentage();

        boolean acceptedAsLastIteration =
                nextIterationNumber >= estimation.getMaximumIterations();

        boolean finalIteration = acceptedByDeviation || acceptedAsLastIteration;

        iteration.setMinimumModuleDeviationPercentage(minimumDeviationPercentage);
        iteration.setMaximumModuleDeviationPercentage(maximumDeviationPercentage);
        iteration.setAcceptedByDeviation(acceptedByDeviation);
        iteration.setAcceptedAsLastIteration(acceptedAsLastIteration && !acceptedByDeviation);
        iteration.setFinalIteration(finalIteration);

        estimation.addIteration(iteration);

        if (finalIteration) {
            double minimumConsensusEffort =
                    calculateAverageMinimumModuleEffort(iteration.getExpertEstimates());

            double maximumConsensusEffort =
                    calculateAverageMaximumModuleEffort(iteration.getExpertEstimates());

            applyFinalCalibrationInternal(
                    estimation,
                    minimumConsensusEffort,
                    maximumConsensusEffort
            );
        }

        return delphiEstimationRepository.save(estimation);
    }

    public Optional<DelphiEstimation> findByIdAndProjectId(Long delphiEstimationId, Long projectId) {
        return delphiEstimationRepository.findByIdAndEstimationProjectId(delphiEstimationId, projectId);
    }

    @Transactional(readOnly = true)
    public Optional<DelphiEstimation> findDetailedByIdAndProjectId(Long delphiEstimationId, Long projectId) {
        Optional<DelphiEstimation> optionalEstimation =
                delphiEstimationRepository.findByIdAndEstimationProjectId(delphiEstimationId, projectId);

        optionalEstimation.ifPresent(this::initializeIterations);

        return optionalEstimation;
    }

    @Transactional(readOnly = true)
    public Optional<DelphiEstimation> findDetailedActiveBySourceAnalysis(SizeAnalysis sourceAnalysis) {
        Optional<DelphiEstimation> optionalEstimation =
                delphiEstimationRepository
                        .findFirstBySourceAnalysisIdAndSourceTechniqueCodeAndActiveTrueOrderByCreatedAtDesc(
                                sourceAnalysis.getId(),
                                sourceAnalysis.getTechniqueCode()
                        );

        optionalEstimation.ifPresent(this::initializeIterations);

        return optionalEstimation;
    }

    @Transactional
    public void deleteAllBySourceAnalysis(SizeAnalysis sourceAnalysis) {
        List<DelphiEstimation> estimations =
                delphiEstimationRepository.findBySourceAnalysisIdAndSourceTechniqueCodeOrderByCreatedAtDesc(
                        sourceAnalysis.getId(),
                        sourceAnalysis.getTechniqueCode()
                );

        if (estimations.isEmpty()) {
            return;
        }

        delphiEstimationRepository.deleteAll(estimations);
    }

    private void initializeIterations(DelphiEstimation estimation) {
        Hibernate.initialize(estimation.getIterations());

        estimation.getIterations().forEach(iteration -> {
            Hibernate.initialize(iteration.getExpertEstimates());
        });

        estimation.getIterations().sort(
                Comparator.comparingInt(DelphiIteration::getIterationNumber)
        );
    }

    private void deactivatePreviousActiveEstimations(SizeAnalysis sourceAnalysis) {
        List<DelphiEstimation> previousEstimations =
                delphiEstimationRepository.findBySourceAnalysisIdAndSourceTechniqueCodeOrderByCreatedAtDesc(
                        sourceAnalysis.getId(),
                        sourceAnalysis.getTechniqueCode()
                );

        for (DelphiEstimation previousEstimation : previousEstimations) {
            if (Boolean.TRUE.equals(previousEstimation.getActive())) {
                previousEstimation.setActive(false);
            }
        }

        delphiEstimationRepository.saveAll(previousEstimations);
    }

    private ModuleReference findMinimumModule(List<EstimationModule> projectModules,
                                              Map<Long, Double> moduleSizeById) {
        return projectModules.stream()
                .map(module -> toModuleReference(module, moduleSizeById))
                .filter(reference -> reference.moduleSize() > 0)
                .min(Comparator.comparingDouble(ModuleReference::moduleSize))
                .orElseThrow(() -> new IllegalStateException("DELPHI_MINIMUM_MODULE_NOT_AVAILABLE"));
    }

    private ModuleReference findMaximumModule(List<EstimationModule> projectModules,
                                              Map<Long, Double> moduleSizeById) {
        return projectModules.stream()
                .map(module -> toModuleReference(module, moduleSizeById))
                .filter(reference -> reference.moduleSize() > 0)
                .max(Comparator.comparingDouble(ModuleReference::moduleSize))
                .orElseThrow(() -> new IllegalStateException("DELPHI_MAXIMUM_MODULE_NOT_AVAILABLE"));
    }

    private ModuleReference toModuleReference(EstimationModule module,
                                              Map<Long, Double> moduleSizeById) {
        Double moduleSize = moduleSizeById.get(module.getId());

        if (moduleSize == null) {
            throw new IllegalStateException("DELPHI_MODULE_SIZE_NOT_AVAILABLE");
        }

        return new ModuleReference(
                module.getId(),
                module.getName(),
                moduleSize
        );
    }

    private void assertValidSourceAnalysis(SizeAnalysis sourceAnalysis) {
        if (sourceAnalysis == null) {
            throw new IllegalArgumentException("SIZE_ANALYSIS_REQUIRED");
        }

        if (sourceAnalysis.getId() == null) {
            throw new IllegalArgumentException("SIZE_ANALYSIS_NOT_PERSISTED");
        }

        if (sourceAnalysis.getEstimationProject() == null
                || sourceAnalysis.getEstimationProject().getId() == null) {
            throw new IllegalArgumentException("SIZE_ANALYSIS_PROJECT_NOT_PERSISTED");
        }

        if (sourceAnalysis.getCalculatedSizeValue() == null || sourceAnalysis.getCalculatedSizeValue() <= 0) {
            throw new IllegalArgumentException("SIZE_ANALYSIS_VALUE_INVALID");
        }

        if (sourceAnalysis.getTechniqueCode() == null || sourceAnalysis.getTechniqueCode().isBlank()) {
            throw new IllegalArgumentException("SIZE_ANALYSIS_TECHNIQUE_REQUIRED");
        }

        if (sourceAnalysis.getSizeUnitCode() == null || sourceAnalysis.getSizeUnitCode().isBlank()) {
            throw new IllegalArgumentException("SIZE_ANALYSIS_UNIT_REQUIRED");
        }
    }

    private void assertPositive(Double value, String errorCode) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(errorCode);
        }
    }

    private void applyFinalCalibrationInternal(DelphiEstimation estimation,
                                               Double minimumModuleEstimatedEffortHours,
                                               Double maximumModuleEstimatedEffortHours) {
        assertPositive(minimumModuleEstimatedEffortHours, "DELPHI_MINIMUM_EFFORT_INVALID");
        assertPositive(maximumModuleEstimatedEffortHours, "DELPHI_MAXIMUM_EFFORT_INVALID");

        double minSize = estimation.getMinimumModuleSizeSnapshot();
        double maxSize = estimation.getMaximumModuleSizeSnapshot();

        if (Double.compare(minSize, maxSize) == 0) {
            throw new IllegalStateException("DELPHI_REGRESSION_REQUIRES_DIFFERENT_SIZES");
        }

        double slope =
                (maximumModuleEstimatedEffortHours - minimumModuleEstimatedEffortHours) / (maxSize - minSize);

        double intercept =
                minimumModuleEstimatedEffortHours - (slope * minSize);

        estimation.setMinimumModuleEstimatedEffortHours(minimumModuleEstimatedEffortHours);
        estimation.setMaximumModuleEstimatedEffortHours(maximumModuleEstimatedEffortHours);
        estimation.setRegressionSlope(slope);
        estimation.setRegressionIntercept(intercept);
    }

    private void assertExtremeModules(ModuleReference minimumModule,
                                      ModuleReference maximumModule) {
        if (minimumModule.moduleId().equals(maximumModule.moduleId())) {
            throw new IllegalStateException("DELPHI_REQUIRES_TWO_DIFFERENT_MODULES");
        }

        if (Double.compare(minimumModule.moduleSize(), maximumModule.moduleSize()) == 0) {
            throw new IllegalStateException("DELPHI_REQUIRES_DIFFERENT_EXTREME_SIZES");
        }
    }

    private void assertEstimationOpenForIteration(DelphiEstimation estimation) {
        if (Boolean.FALSE.equals(estimation.getActive())) {
            throw new IllegalStateException("DELPHI_ESTIMATION_INACTIVE");
        }

        if (isFinished(estimation)) {
            throw new IllegalStateException("DELPHI_ESTIMATION_ALREADY_FINALIZED");
        }
    }

    private void assertCalibrationAvailable(DelphiEstimation estimation) {
        if (!isFinished(estimation)) {
            throw new IllegalStateException("DELPHI_CALIBRATION_NOT_AVAILABLE");
        }
    }

    private void assertExpertEstimatesReady(List<DelphiExpertEstimate> expertEstimates) {
        if (expertEstimates == null || expertEstimates.size() < 3) {
            throw new IllegalArgumentException("DELPHI_EXPERT_ESTIMATES_MINIMUM_NOT_MET");
        }

        Set<String> evaluatorAliases = new HashSet<>();

        for (DelphiExpertEstimate expertEstimate : expertEstimates) {
            String alias = normalizeText(expertEstimate.getEvaluatorAlias());

            if (alias == null || alias.isBlank()) {
                throw new IllegalArgumentException("DELPHI_EXPERT_ALIAS_REQUIRED");
            }

            if (!evaluatorAliases.add(alias.toLowerCase())) {
                throw new IllegalArgumentException("DELPHI_EXPERT_ALIAS_DUPLICATED");
            }

            assertPositive(
                    expertEstimate.getMinimumModuleEstimatedEffortHours(),
                    "DELPHI_MINIMUM_MODULE_EFFORT_INVALID"
            );

            assertPositive(
                    expertEstimate.getMaximumModuleEstimatedEffortHours(),
                    "DELPHI_MAXIMUM_MODULE_EFFORT_INVALID"
            );
        }
    }

    private double calculateDeviationPercentageForMinimumModule(List<DelphiExpertEstimate> expertEstimates) {
        List<Double> values = expertEstimates.stream()
                .map(DelphiExpertEstimate::getMinimumModuleEstimatedEffortHours)
                .toList();

        return calculateDeviationPercentage(values);
    }

    private double calculateDeviationPercentageForMaximumModule(List<DelphiExpertEstimate> expertEstimates) {
        List<Double> values = expertEstimates.stream()
                .map(DelphiExpertEstimate::getMaximumModuleEstimatedEffortHours)
                .toList();

        return calculateDeviationPercentage(values);
    }

    private double calculateDeviationPercentage(List<Double> values) {
        double min = values.stream()
                .mapToDouble(Double::doubleValue)
                .min()
                .orElseThrow(() -> new IllegalStateException("DELPHI_DEVIATION_VALUES_EMPTY"));

        double max = values.stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElseThrow(() -> new IllegalStateException("DELPHI_DEVIATION_VALUES_EMPTY"));

        double average = values.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElseThrow(() -> new IllegalStateException("DELPHI_DEVIATION_VALUES_EMPTY"));

        if (Double.compare(average, 0.0) == 0) {
            throw new IllegalStateException("DELPHI_DEVIATION_AVERAGE_ZERO");
        }

        return ((max - min) / average) * 100.0;
    }

    private double calculateAverageMinimumModuleEffort(List<DelphiExpertEstimate> expertEstimates) {
        return expertEstimates.stream()
                .mapToDouble(DelphiExpertEstimate::getMinimumModuleEstimatedEffortHours)
                .average()
                .orElseThrow(() -> new IllegalStateException("DELPHI_MINIMUM_MODULE_AVERAGE_NOT_AVAILABLE"));
    }

    private double calculateAverageMaximumModuleEffort(List<DelphiExpertEstimate> expertEstimates) {
        return expertEstimates.stream()
                .mapToDouble(DelphiExpertEstimate::getMaximumModuleEstimatedEffortHours)
                .average()
                .orElseThrow(() -> new IllegalStateException("DELPHI_MAXIMUM_MODULE_AVERAGE_NOT_AVAILABLE"));
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private record ModuleReference(Long moduleId, String moduleName, Double moduleSize) {
    }
}