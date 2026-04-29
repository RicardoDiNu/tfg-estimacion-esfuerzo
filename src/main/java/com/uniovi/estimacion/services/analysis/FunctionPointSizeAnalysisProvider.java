package com.uniovi.estimacion.services.analysis;

import com.uniovi.estimacion.common.codes.SizeTechniqueCodes;
import com.uniovi.estimacion.common.codes.SizeUnitCodes;
import com.uniovi.estimacion.entities.analysis.SizeAnalysis;
import com.uniovi.estimacion.entities.functionpoints.FunctionPointAnalysis;
import com.uniovi.estimacion.entities.functionpoints.functions.DataFunction;
import com.uniovi.estimacion.entities.functionpoints.functions.TransactionalFunction;
import com.uniovi.estimacion.entities.projects.EstimationModule;
import com.uniovi.estimacion.services.functionpoints.FunctionPointAnalysisService;
import com.uniovi.estimacion.services.functionpoints.FunctionPointAnalysisSummary;
import com.uniovi.estimacion.services.functionpoints.FunctionPointCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FunctionPointSizeAnalysisProvider implements SizeAnalysisProvider {

    private final FunctionPointAnalysisService functionPointAnalysisService;
    private final FunctionPointCalculationService functionPointCalculationService;

    @Override
    public String getTechniqueCode() {
        return SizeTechniqueCodes.FUNCTION_POINTS;
    }

    @Override
    public String getSizeUnitCode() {
        return SizeUnitCodes.FP;
    }

    @Override
    public Optional<? extends SizeAnalysis> findDetailedByProjectId(Long projectId) {
        return functionPointAnalysisService.findDetailedByProjectId(projectId);
    }

    @Override
    public Map<Long, Double> buildModuleSizeById(SizeAnalysis analysis,
                                                 List<EstimationModule> modulesList) {
        if (!(analysis instanceof FunctionPointAnalysis functionPointAnalysis)) {
            throw new IllegalArgumentException("El análisis recibido no es un análisis de Puntos Función.");
        }

        Map<Long, Double> moduleSizeById = new LinkedHashMap<>();

        for (EstimationModule module : modulesList) {
            List<DataFunction> moduleDataFunctions =
                    functionPointAnalysisService.findAllDataFunctionsByModuleId(module.getId());

            List<TransactionalFunction> moduleTransactionalFunctions =
                    functionPointAnalysisService.findAllTransactionalFunctionsByModuleId(module.getId());

            FunctionPointAnalysisSummary moduleSummary =
                    functionPointCalculationService.buildModuleSummary(
                            functionPointAnalysis,
                            moduleDataFunctions,
                            moduleTransactionalFunctions
                    );

            moduleSizeById.put(module.getId(), moduleSummary.getAdjustedFunctionPoints());
        }

        return moduleSizeById;
    }

    @Override
    public String getDetailsPath(Long projectId) {
        return "/projects/" + projectId + "/function-points";
    }

    @Override
    public String getAddPath(Long projectId) {
        return "/projects/" + projectId + "/function-points/add";
    }
}