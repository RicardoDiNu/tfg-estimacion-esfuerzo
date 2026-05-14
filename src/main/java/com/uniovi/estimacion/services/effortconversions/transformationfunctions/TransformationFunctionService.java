package com.uniovi.estimacion.services.effortconversions.transformationfunctions;

import com.uniovi.estimacion.entities.effortconversions.transformationfunctions.TransformationFunction;
import com.uniovi.estimacion.entities.effortconversions.transformationfunctions.TransformationFunctionConversion;
import com.uniovi.estimacion.entities.sizeanalyses.SizeAnalysis;
import com.uniovi.estimacion.entities.users.User;
import com.uniovi.estimacion.repositories.effortconversions.transformationfunctions.TransformationFunctionConversionRepository;
import com.uniovi.estimacion.repositories.effortconversions.transformationfunctions.TransformationFunctionRepository;
import com.uniovi.estimacion.services.effortconversions.LinearEffortModel;
import com.uniovi.estimacion.services.users.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransformationFunctionService {

    private final TransformationFunctionRepository transformationFunctionRepository;
    private final TransformationFunctionConversionRepository transformationFunctionConversionRepository;
    private final CurrentUserService currentUserService;

    public List<TransformationFunction> findAvailableFunctions(SizeAnalysis sourceAnalysis) {
        validateSourceAnalysis(sourceAnalysis);

        List<TransformationFunction> availableFunctions = new ArrayList<>(
                transformationFunctionRepository
                        .findByActiveTrueAndSourceTechniqueCodeAndSourceSizeUnitCodeAndPredefinedTrueOrderByNameAsc(
                                sourceAnalysis.getTechniqueCode(),
                                sourceAnalysis.getSizeUnitCode()
                        )
        );

        currentUserService.getCurrentUser()
                .map(User::getId)
                .ifPresent(ownerId -> availableFunctions.addAll(
                        transformationFunctionRepository
                                .findByActiveTrueAndSourceTechniqueCodeAndSourceSizeUnitCodeAndOwnerIdOrderByNameAsc(
                                        sourceAnalysis.getTechniqueCode(),
                                        sourceAnalysis.getSizeUnitCode(),
                                        ownerId
                                )
                ));

        availableFunctions.sort(Comparator.comparing(TransformationFunction::getName));

        return availableFunctions;
    }

    public Optional<TransformationFunctionConversion> findActiveBySourceAnalysis(SizeAnalysis sourceAnalysis) {
        validateSourceAnalysis(sourceAnalysis);

        Optional<TransformationFunctionConversion> optionalConversion =
                transformationFunctionConversionRepository
                        .findFirstBySourceAnalysisIdAndSourceTechniqueCodeAndActiveTrueOrderByCreatedAtDesc(
                                sourceAnalysis.getId(),
                                sourceAnalysis.getTechniqueCode()
                        );

        optionalConversion.ifPresent(this::initializeFunction);

        return optionalConversion;
    }

    public List<TransformationFunctionConversion> findHistoryBySourceAnalysis(SizeAnalysis sourceAnalysis) {
        validateSourceAnalysis(sourceAnalysis);

        List<TransformationFunctionConversion> conversions =
                transformationFunctionConversionRepository
                        .findBySourceAnalysisIdAndSourceTechniqueCodeOrderByCreatedAtDesc(
                                sourceAnalysis.getId(),
                                sourceAnalysis.getTechniqueCode()
                        );

        conversions.forEach(this::initializeFunction);

        return conversions;
    }

    public Optional<TransformationFunctionConversion> findByIdAndProjectId(Long conversionId, Long projectId) {
        Optional<TransformationFunctionConversion> optionalConversion =
                transformationFunctionConversionRepository.findByIdAndEstimationProjectId(conversionId, projectId);

        optionalConversion.ifPresent(this::initializeFunction);

        return optionalConversion;
    }

    @Transactional
    public TransformationFunction createCustomFunction(SizeAnalysis sourceAnalysis,
                                                       String name,
                                                       String description,
                                                       Double intercept,
                                                       Double slope) {
        validateSourceAnalysis(sourceAnalysis);
        validateLinearParameters(intercept, slope);

        User owner = currentUserService.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Debe existir un usuario autenticado."));

        TransformationFunction function = new TransformationFunction();
        function.setName(normalizeText(name));
        function.setDescription(normalizeText(description));
        function.setSourceTechniqueCode(sourceAnalysis.getTechniqueCode());
        function.setSourceSizeUnitCode(sourceAnalysis.getSizeUnitCode());
        function.setIntercept(intercept);
        function.setSlope(slope);
        function.setPredefined(false);
        function.setOwner(owner);
        function.setActive(true);

        return transformationFunctionRepository.save(function);
    }

    @Transactional
    public TransformationFunction createPredefinedFunction(String name,
                                                           String description,
                                                           String sourceTechniqueCode,
                                                           String sourceSizeUnitCode,
                                                           Double intercept,
                                                           Double slope) {
        validateLinearParameters(intercept, slope);

        if (transformationFunctionRepository
                .existsByNameAndSourceTechniqueCodeAndSourceSizeUnitCodeAndPredefinedTrue(
                        name,
                        sourceTechniqueCode,
                        sourceSizeUnitCode
                )) {
            return transformationFunctionRepository
                    .findByActiveTrueAndSourceTechniqueCodeAndSourceSizeUnitCodeAndPredefinedTrueOrderByNameAsc(
                            sourceTechniqueCode,
                            sourceSizeUnitCode
                    )
                    .stream()
                    .filter(function -> function.getName().equals(name))
                    .findFirst()
                    .orElseThrow();
        }

        TransformationFunction function = new TransformationFunction();
        function.setName(normalizeText(name));
        function.setDescription(normalizeText(description));
        function.setSourceTechniqueCode(sourceTechniqueCode);
        function.setSourceSizeUnitCode(sourceSizeUnitCode);
        function.setIntercept(intercept);
        function.setSlope(slope);
        function.setPredefined(true);
        function.setOwner(null);
        function.setActive(true);

        return transformationFunctionRepository.save(function);
    }

    @Transactional
    public TransformationFunctionConversion createConversion(SizeAnalysis sourceAnalysis,
                                                             Long transformationFunctionId) {
        validateSourceAnalysis(sourceAnalysis);

        TransformationFunction function = transformationFunctionRepository.findById(transformationFunctionId)
                .orElseThrow(() -> new IllegalArgumentException("No existe la función de transformación seleccionada."));

        if (!function.getActive()) {
            throw new IllegalArgumentException("La función de transformación seleccionada no está activa.");
        }

        if (!function.isApplicableTo(sourceAnalysis.getTechniqueCode(), sourceAnalysis.getSizeUnitCode())) {
            throw new IllegalArgumentException("La función de transformación seleccionada no es aplicable al análisis de tamaño.");
        }

        deactivatePreviousActiveConversions(sourceAnalysis);

        TransformationFunctionConversion conversion = new TransformationFunctionConversion();
        conversion.setEstimationProject(sourceAnalysis.getEstimationProject());

        conversion.setSourceAnalysisId(sourceAnalysis.getId());
        conversion.setSourceTechniqueCode(sourceAnalysis.getTechniqueCode());
        conversion.setSourceSizeUnitCode(sourceAnalysis.getSizeUnitCode());
        conversion.setSourceProjectSizeSnapshot(sourceAnalysis.getCalculatedSizeValue());

        conversion.setTransformationFunction(function);
        conversion.setFunctionNameSnapshot(function.getName());
        conversion.setInterceptSnapshot(function.getIntercept());
        conversion.setSlopeSnapshot(function.getSlope());

        conversion.setActive(true);
        conversion.setOutdated(false);

        return transformationFunctionConversionRepository.save(conversion);
    }

    @Transactional
    public void deleteByIdAndProjectId(Long conversionId, Long projectId) {
        transformationFunctionConversionRepository.findByIdAndEstimationProjectId(conversionId, projectId)
                .ifPresent(transformationFunctionConversionRepository::delete);
    }

    @Transactional
    public void deleteAllConversionsByProjectId(Long projectId) {
        transformationFunctionConversionRepository.deleteByEstimationProjectId(projectId);
    }

    public double calculateEstimatedEffortHours(TransformationFunctionConversion conversion,
                                                Double currentSize) {
        if (conversion == null || !conversion.isFinished()) {
            throw new IllegalStateException("La conversión por función de transformación no está completa.");
        }

        LinearEffortModel model = new LinearEffortModel(
                conversion.getInterceptSnapshot(),
                conversion.getSlopeSnapshot()
        );

        return model.estimate(currentSize);
    }

    public double calculateEstimatedEffortHours(TransformationFunction function,
                                                Double currentSize) {
        if (function == null) {
            throw new IllegalArgumentException("La función de transformación no puede ser nula.");
        }

        LinearEffortModel model = new LinearEffortModel(
                function.getIntercept(),
                function.getSlope()
        );

        return model.estimate(currentSize);
    }

    private void deactivatePreviousActiveConversions(SizeAnalysis sourceAnalysis) {
        transformationFunctionConversionRepository
                .findFirstBySourceAnalysisIdAndSourceTechniqueCodeAndActiveTrueOrderByCreatedAtDesc(
                        sourceAnalysis.getId(),
                        sourceAnalysis.getTechniqueCode()
                )
                .ifPresent(previousConversion -> {
                    previousConversion.setActive(false);
                    transformationFunctionConversionRepository.save(previousConversion);
                });
    }

    private void validateSourceAnalysis(SizeAnalysis sourceAnalysis) {
        if (sourceAnalysis == null || sourceAnalysis.getId() == null) {
            throw new IllegalArgumentException("El análisis de tamaño origen no es válido.");
        }

        if (sourceAnalysis.getCalculatedSizeValue() == null) {
            throw new IllegalArgumentException("El análisis de tamaño no tiene un tamaño calculado.");
        }
    }

    private void validateLinearParameters(Double intercept, Double slope) {
        if (intercept == null || intercept < 0) {
            throw new IllegalArgumentException("El parámetro a debe ser mayor o igual que cero.");
        }

        if (slope == null || slope <= 0) {
            throw new IllegalArgumentException("El parámetro b debe ser mayor que cero.");
        }
    }

    private void initializeFunction(TransformationFunctionConversion conversion) {
        Hibernate.initialize(conversion.getTransformationFunction());
    }

    private String normalizeText(String value) {
        return value != null ? value.trim() : null;
    }
}