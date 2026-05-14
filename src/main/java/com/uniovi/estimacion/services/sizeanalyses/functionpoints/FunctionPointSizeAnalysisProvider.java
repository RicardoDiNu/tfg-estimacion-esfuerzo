package com.uniovi.estimacion.services.sizeanalyses.functionpoints;

import com.uniovi.estimacion.common.codes.SizeTechniqueCodes;
import com.uniovi.estimacion.common.codes.SizeUnitCodes;
import com.uniovi.estimacion.entities.sizeanalyses.SizeAnalysis;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.FunctionPointAnalysis;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.DataFunction;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.TransactionalFunction;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.modules.FunctionPointModule;
import com.uniovi.estimacion.services.sizeanalyses.SizeAnalysisModuleResult;
import com.uniovi.estimacion.services.sizeanalyses.SizeAnalysisProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class FunctionPointSizeAnalysisProvider implements SizeAnalysisProvider {

    private final FunctionPointAnalysisService functionPointAnalysisService;
    private final FunctionPointCalculationService functionPointCalculationService;
    private final FunctionPointModuleService functionPointModuleService;

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
    public List<SizeAnalysisModuleResult> buildModuleResults(SizeAnalysis analysis) {
        if (!(analysis instanceof FunctionPointAnalysis functionPointAnalysis)) {
            throw new IllegalArgumentException("El análisis recibido no es un análisis de Puntos Función.");
        }

        Long projectId = functionPointAnalysis.getEstimationProject().getId();
        List<FunctionPointModule> modulesList = functionPointModuleService.findAllByProjectId(projectId);

        List<SizeAnalysisModuleResult> moduleResults = new ArrayList<>();

        for (FunctionPointModule module : modulesList) {
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

            moduleResults.add(new SizeAnalysisModuleResult(
                    module.getId(),
                    module.getName(),
                    moduleSummary.getAdjustedFunctionPoints()
            ));
        }

        return moduleResults;
    }

    @Override
    public Map<Long, Double> buildModuleSizeById(SizeAnalysis analysis,
                                                 List<FunctionPointModule> modulesList) {
        if (!(analysis instanceof FunctionPointAnalysis functionPointAnalysis)) {
            throw new IllegalArgumentException("El análisis recibido no es un análisis de Puntos Función.");
        }

        Map<Long, Double> moduleSizeById = new LinkedHashMap<>();

        for (FunctionPointModule module : modulesList) {
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