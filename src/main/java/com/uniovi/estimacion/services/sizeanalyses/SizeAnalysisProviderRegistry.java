package com.uniovi.estimacion.services.sizeanalyses;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SizeAnalysisProviderRegistry {

    private final Map<String, SizeAnalysisProvider> providersByTechniqueCode;

    public SizeAnalysisProviderRegistry(List<SizeAnalysisProvider> providers) {
        this.providersByTechniqueCode = providers.stream()
                .collect(Collectors.toMap(
                        SizeAnalysisProvider::getTechniqueCode,
                        Function.identity()
                ));
    }

    public Optional<SizeAnalysisProvider> findByTechniqueCode(String techniqueCode) {
        return Optional.ofNullable(providersByTechniqueCode.get(techniqueCode));
    }

    public SizeAnalysisProvider getByTechniqueCode(String techniqueCode) {
        return findByTechniqueCode(techniqueCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe proveedor de análisis de tamaño para la técnica: " + techniqueCode
                ));
    }
}