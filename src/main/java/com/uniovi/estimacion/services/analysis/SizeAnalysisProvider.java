package com.uniovi.estimacion.services.analysis;

import com.uniovi.estimacion.entities.analysis.SizeAnalysis;
import com.uniovi.estimacion.entities.projects.EstimationModule;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SizeAnalysisProvider {

    String getTechniqueCode();

    String getSizeUnitCode();

    Optional<? extends SizeAnalysis> findDetailedByProjectId(Long projectId);

    Map<Long, Double> buildModuleSizeById(SizeAnalysis analysis,
                                          List<EstimationModule> modulesList);

    String getDetailsPath(Long projectId);

    String getAddPath(Long projectId);
}