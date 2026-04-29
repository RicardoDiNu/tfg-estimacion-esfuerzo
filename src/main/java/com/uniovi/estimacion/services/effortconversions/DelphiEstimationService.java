package com.uniovi.estimacion.services.effortconversions;

import com.uniovi.estimacion.entities.analysis.SizeAnalysis;
import com.uniovi.estimacion.entities.effortconversions.DelphiEstimation;
import com.uniovi.estimacion.entities.effortconversions.DelphiExpertEstimate;
import com.uniovi.estimacion.entities.effortconversions.DelphiIteration;
import com.uniovi.estimacion.entities.projects.EstimationModule;
import com.uniovi.estimacion.repositories.effortconversions.DelphiEstimationRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DelphiEstimationService {

    private static final int MINIMUM_EXPERT_COUNT = 3;

    private final DelphiEstimationRepository delphiEstimationRepository;

    public Optional<DelphiEstimation> findActiveBySourceAnalysis(SizeAnalysis sourceAnalysis) {
        return delphiEstimationRepository
                .findFirstBySourceAnalysisIdAndSourceTechniqueCodeAndActiveTrueOrderByCreatedAtDesc(
                        sourceAnalysis.getId(),
                        sourceAnalysis.getTechniqueCode()
                );
    }

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

    public Optional<DelphiEstimation> findByIdAndProjectId(Long delphiEstimationId, Long projectId) {
        return delphiEstimationRepository.findByIdAndEstimationProjectId(delphiEstimationId, projectId);
    }

    public Optional<DelphiEstimation> findDetailedByIdAndProjectId(Long delphiEstimationId, Long projectId) {
        Optional<DelphiEstimation> optionalEstimation =
                delphiEstimationRepository.findByIdAndEstimationProjectId(delphiEstimationId, projectId);

        optionalEstimation.ifPresent(this::initializeIterations);

        return optionalEstimation;
    }

    public boolean canStartCalibration(Map<Long, Double> moduleSizeById) {
        long distinctPositiveModuleCount = moduleSizeById.values().stream()
                .filter(size -> size != null && size > 0)
                .count();

        long distinctPositiveSizes = moduleSizeById.values().stream()
                .filter(size -> size != null && size > 0)
                .distinct()
                .count();

        return distinctPositiveModuleCount >= 2 && distinctPositiveSizes >= 2;
    }

    public boolean isFinished(DelphiEstimation estimation) {
        return estimation != null && estimation.isFinished();
    }



    @Transactional
    public DelphiEstimation createInitialEstimation(SizeAnalysis sourceAnalysis,
                                                    List<EstimationModule> projectModules,
                                                    Map<Long, Double> moduleSizeById,
                                                    Double acceptableDeviationPercentage,
                                                    Integer maximumIterations,
                                                    Integer expertCount) {
        validateSourceAnalysis(sourceAnalysis);
        validateInitialConfiguration(acceptableDeviationPercentage, maximumIterations, expertCount);

        ModuleReference minimumModule = findMinimumModule(projectModules, moduleSizeById);
        ModuleReference maximumModule = findMaximumModule(projectModules, moduleSizeById);

        validateExtremeModules(minimumModule, maximumModule);

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

        estimation.setAcceptableDeviationPercentage(acceptableDeviationPercentage);
        estimation.setMaximumIterations(maximumIterations);
        estimation.setExpertCount(expertCount);

        estimation.setActive(true);
        estimation.setOutdated(false);

        return delphiEstimationRepository.save(estimation);
    }

    @Transactional
    public DelphiEstimation registerIteration(Long delphiEstimationId,
                                              List<DelphiExpertEstimate> expertEstimates) {
        DelphiEstimation estimation = delphiEstimationRepository.findById(delphiEstimationId)
                .orElseThrow(() -> new IllegalArgumentException("No existe una conversión Delphi con ese id."));

        validateEstimationForIteration(estimation);
        validateExpertEstimates(estimation, expertEstimates);

        int nextIterationNumber = estimation.getIterations().size() + 1;

        DelphiIteration iteration = new DelphiIteration();
        iteration.setIterationNumber(nextIterationNumber);

        for (DelphiExpertEstimate incomingEstimate : expertEstimates) {
            DelphiExpertEstimate storedEstimate = new DelphiExpertEstimate();
            storedEstimate.setEvaluatorAlias(normalizeText(incomingEstimate.getEvaluatorAlias()));
            storedEstimate.setMinimumModuleEstimatedEffortHours(incomingEstimate.getMinimumModuleEstimatedEffortHours());
            storedEstimate.setMaximumModuleEstimatedEffortHours(incomingEstimate.getMaximumModuleEstimatedEffortHours());
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
        if (!isFinished(estimation)) {
            throw new IllegalStateException("La conversión Delphi todavía no tiene una función lineal calculada.");
        }

        LinearEffortModel model = new LinearEffortModel(
                estimation.getRegressionIntercept(),
                estimation.getRegressionSlope()
        );

        return model.estimate(moduleSize);
    }

    public double calculateTotalEstimatedEffortHours(DelphiEstimation estimation,
                                                     Map<Long, Double> moduleSizeById) {
        return moduleSizeById.values().stream()
                .filter(size -> size != null && size >= 0)
                .mapToDouble(size -> calculateEstimatedEffortHours(estimation, size))
                .sum();
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

    @Transactional
    public boolean deleteByIdAndProjectId(Long delphiEstimationId, Long projectId) {
        Optional<DelphiEstimation> optionalEstimation =
                delphiEstimationRepository.findByIdAndEstimationProjectId(delphiEstimationId, projectId);

        if (optionalEstimation.isEmpty()) {
            return false;
        }

        delphiEstimationRepository.delete(optionalEstimation.get());
        return true;
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
        List<DelphiEstimation> previousEstimations = delphiEstimationRepository
                .findBySourceAnalysisIdAndSourceTechniqueCodeOrderByCreatedAtDesc(
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
                .orElseThrow(() -> new IllegalStateException(
                        "No existen suficientes módulos con tamaño calculado para iniciar Delphi."
                ));
    }

    private ModuleReference findMaximumModule(List<EstimationModule> projectModules,
                                              Map<Long, Double> moduleSizeById) {
        return projectModules.stream()
                .map(module -> toModuleReference(module, moduleSizeById))
                .filter(reference -> reference.moduleSize() > 0)
                .max(Comparator.comparingDouble(ModuleReference::moduleSize))
                .orElseThrow(() -> new IllegalStateException(
                        "No existen suficientes módulos con tamaño calculado para iniciar Delphi."
                ));
    }

    private ModuleReference toModuleReference(EstimationModule module,
                                              Map<Long, Double> moduleSizeById) {
        Double moduleSize = moduleSizeById.get(module.getId());

        if (moduleSize == null) {
            throw new IllegalStateException(
                    "No existe tamaño calculado para el módulo con id " + module.getId()
            );
        }

        return new ModuleReference(
                module.getId(),
                module.getName(),
                moduleSize
        );
    }

    private void validateSourceAnalysis(SizeAnalysis sourceAnalysis) {
        if (sourceAnalysis == null) {
            throw new IllegalArgumentException("El análisis de tamaño origen es obligatorio.");
        }

        if (sourceAnalysis.getId() == null) {
            throw new IllegalArgumentException("El análisis de tamaño origen debe estar persistido.");
        }

        if (sourceAnalysis.getEstimationProject() == null || sourceAnalysis.getEstimationProject().getId() == null) {
            throw new IllegalArgumentException("El análisis de tamaño debe pertenecer a un proyecto persistido.");
        }

        if (sourceAnalysis.getCalculatedSizeValue() == null || sourceAnalysis.getCalculatedSizeValue() <= 0) {
            throw new IllegalArgumentException("El análisis de tamaño debe tener un tamaño calculado positivo.");
        }

        if (sourceAnalysis.getTechniqueCode() == null || sourceAnalysis.getTechniqueCode().isBlank()) {
            throw new IllegalArgumentException("El código de técnica de tamaño es obligatorio.");
        }

        if (sourceAnalysis.getSizeUnitCode() == null || sourceAnalysis.getSizeUnitCode().isBlank()) {
            throw new IllegalArgumentException("La unidad de tamaño es obligatoria.");
        }
    }

    private void validateInitialConfiguration(Double acceptableDeviationPercentage,
                                              Integer maximumIterations,
                                              Integer expertCount) {
        if (acceptableDeviationPercentage == null || acceptableDeviationPercentage <= 0 || acceptableDeviationPercentage > 100) {
            throw new IllegalArgumentException("La desviación aceptable debe ser mayor que 0 y menor o igual que 100.");
        }

        if (maximumIterations == null || maximumIterations < 1) {
            throw new IllegalArgumentException("El número máximo de iteraciones debe ser al menos 1.");
        }

        if (expertCount == null || expertCount < MINIMUM_EXPERT_COUNT) {
            throw new IllegalArgumentException("El número de expertos debe ser al menos " + MINIMUM_EXPERT_COUNT + ".");
        }
    }

    private void validateEffortValue(Double value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("El campo " + fieldName + " debe ser un valor positivo.");
        }
    }

    private void applyFinalCalibrationInternal(DelphiEstimation estimation,
                                               Double minimumModuleEstimatedEffortHours,
                                               Double maximumModuleEstimatedEffortHours) {
        validateEffortValue(minimumModuleEstimatedEffortHours, "minimumModuleEstimatedEffortHours");
        validateEffortValue(maximumModuleEstimatedEffortHours, "maximumModuleEstimatedEffortHours");

        double minSize = estimation.getMinimumModuleSizeSnapshot();
        double maxSize = estimation.getMaximumModuleSizeSnapshot();

        if (Double.compare(minSize, maxSize) == 0) {
            throw new IllegalStateException("No se puede calcular la recta Delphi porque los tamaños mínimo y máximo coinciden.");
        }

        LinearEffortModel model = LinearEffortModel.fromTwoPoints(
                minSize,
                minimumModuleEstimatedEffortHours,
                maxSize,
                maximumModuleEstimatedEffortHours
        );

        estimation.setMinimumModuleEstimatedEffortHours(minimumModuleEstimatedEffortHours);
        estimation.setMaximumModuleEstimatedEffortHours(maximumModuleEstimatedEffortHours);
        estimation.setRegressionSlope(model.slope());
        estimation.setRegressionIntercept(model.intercept());
        estimation.setOutdated(false);
    }

    private void validateExtremeModules(ModuleReference minimumModule,
                                        ModuleReference maximumModule) {
        if (minimumModule.moduleId().equals(maximumModule.moduleId())) {
            throw new IllegalStateException("Delphi necesita al menos dos módulos distintos con tamaño calculado.");
        }

        if (Double.compare(minimumModule.moduleSize(), maximumModule.moduleSize()) == 0) {
            throw new IllegalStateException("Delphi necesita módulos extremo con tamaños distintos para calcular la recta.");
        }
    }

    private void validateEstimationForIteration(DelphiEstimation estimation) {
        if (Boolean.FALSE.equals(estimation.getActive())) {
            throw new IllegalStateException("No se puede registrar una iteración sobre una conversión Delphi inactiva.");
        }

        if (Boolean.TRUE.equals(estimation.getOutdated())) {
            throw new IllegalStateException("No se puede registrar una iteración sobre una conversión Delphi desactualizada.");
        }

        if (isFinished(estimation)) {
            throw new IllegalStateException("La conversión Delphi ya tiene una función final calculada.");
        }
    }

    private void validateExpertEstimates(DelphiEstimation estimation,
                                         List<DelphiExpertEstimate> expertEstimates) {
        if (expertEstimates == null || expertEstimates.isEmpty()) {
            throw new IllegalArgumentException("La iteración debe incluir estimaciones de expertos.");
        }

        if (expertEstimates.size() != estimation.getExpertCount()) {
            throw new IllegalArgumentException("La iteración debe incluir exactamente " + estimation.getExpertCount() + " expertos.");
        }

        Set<String> evaluatorAliases = new HashSet<>();

        for (DelphiExpertEstimate expertEstimate : expertEstimates) {
            String alias = normalizeText(expertEstimate.getEvaluatorAlias());

            if (alias == null || alias.isBlank()) {
                throw new IllegalArgumentException("Cada estimación Delphi debe tener un alias de evaluador.");
            }

            if (!evaluatorAliases.add(alias.toLowerCase())) {
                throw new IllegalArgumentException("No puede haber aliases de evaluador repetidos en la misma iteración.");
            }

            validateEffortValue(
                    expertEstimate.getMinimumModuleEstimatedEffortHours(),
                    "minimumModuleEstimatedEffortHours"
            );

            validateEffortValue(
                    expertEstimate.getMaximumModuleEstimatedEffortHours(),
                    "maximumModuleEstimatedEffortHours"
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
                .orElseThrow(() -> new IllegalStateException("No hay valores para calcular la desviación."));

        double max = values.stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElseThrow(() -> new IllegalStateException("No hay valores para calcular la desviación."));

        double average = values.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElseThrow(() -> new IllegalStateException("No hay valores para calcular la desviación."));

        if (Double.compare(average, 0.0) == 0) {
            throw new IllegalStateException("No se puede calcular la desviación con media cero.");
        }

        return ((max - min) / average) * 100.0;
    }

    private double calculateAverageMinimumModuleEffort(List<DelphiExpertEstimate> expertEstimates) {
        return expertEstimates.stream()
                .mapToDouble(DelphiExpertEstimate::getMinimumModuleEstimatedEffortHours)
                .average()
                .orElseThrow(() -> new IllegalStateException("No hay estimaciones para calcular el esfuerzo medio del módulo mínimo."));
    }

    private double calculateAverageMaximumModuleEffort(List<DelphiExpertEstimate> expertEstimates) {
        return expertEstimates.stream()
                .mapToDouble(DelphiExpertEstimate::getMaximumModuleEstimatedEffortHours)
                .average()
                .orElseThrow(() -> new IllegalStateException("No hay estimaciones para calcular el esfuerzo medio del módulo máximo."));
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private record ModuleReference(Long moduleId, String moduleName, Double moduleSize) {
    }
}