package com.uniovi.estimacion.services.sizeanalyses;

import com.uniovi.estimacion.entities.sizeanalyses.SizeAnalysis;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.modules.EstimationModule;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SizeAnalysisProvider {

    String getTechniqueCode();

    String getSizeUnitCode();

    Optional<? extends SizeAnalysis> findDetailedByProjectId(Long projectId);

    List<SizeAnalysisModuleResult> buildModuleResults(SizeAnalysis analysis);

    default Map<Long, Double> buildModuleSizeById(SizeAnalysis analysis) {
        Map<Long, Double> moduleSizeById = new LinkedHashMap<>();

        for (SizeAnalysisModuleResult moduleResult : buildModuleResults(analysis)) {
            moduleSizeById.put(moduleResult.getModuleId(), moduleResult.getSize());
        }

        return moduleSizeById;
    }

    @Deprecated
    default Map<Long, Double> buildModuleSizeById(SizeAnalysis analysis,
                                                  List<EstimationModule> modulesList) {
        return buildModuleSizeById(analysis);
    }

    String getDetailsPath(Long projectId);

    String getAddPath(Long projectId);
}