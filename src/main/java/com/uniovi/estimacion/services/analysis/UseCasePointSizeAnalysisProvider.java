package com.uniovi.estimacion.services.analysis;

import com.uniovi.estimacion.common.codes.SizeTechniqueCodes;
import com.uniovi.estimacion.common.codes.SizeUnitCodes;
import com.uniovi.estimacion.entities.analysis.SizeAnalysis;
import com.uniovi.estimacion.entities.usecasepoints.UseCasePointAnalysis;
import com.uniovi.estimacion.entities.usecasepoints.modules.UseCasePointModule;
import com.uniovi.estimacion.services.usecasepoints.UseCasePointAnalysisService;
import com.uniovi.estimacion.services.usecasepoints.UseCasePointCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UseCasePointSizeAnalysisProvider implements SizeAnalysisProvider {

    private final UseCasePointAnalysisService useCasePointAnalysisService;
    private final UseCasePointCalculationService useCasePointCalculationService;

    @Override
    public String getTechniqueCode() {
        return SizeTechniqueCodes.USE_CASE_POINTS;
    }

    @Override
    public String getSizeUnitCode() {
        return SizeUnitCodes.UCP;
    }

    @Override
    public Optional<? extends SizeAnalysis> findDetailedByProjectId(Long projectId) {
        return useCasePointAnalysisService.findDetailedByProjectId(projectId);
    }

    @Override
    public List<SizeAnalysisModuleResult> buildModuleResults(SizeAnalysis analysis) {
        if (!(analysis instanceof UseCasePointAnalysis useCasePointAnalysis)) {
            throw new IllegalArgumentException("El análisis recibido no es un análisis de Use Case Points.");
        }

        Long projectId = useCasePointAnalysis.getEstimationProject().getId();
        List<UseCasePointModule> modulesList =
                useCasePointAnalysisService.findAllModulesByProjectId(projectId);

        List<SizeAnalysisModuleResult> moduleResults = new ArrayList<>();

        for (UseCasePointModule module : modulesList) {
            double moduleSize =
                    useCasePointCalculationService.calculateAdjustedUseCasePointsForModule(
                            useCasePointAnalysis,
                            module.getUseCases()
                    );

            moduleResults.add(new SizeAnalysisModuleResult(
                    module.getId(),
                    module.getName(),
                    moduleSize
            ));
        }

        return moduleResults;
    }

    @Override
    public String getDetailsPath(Long projectId) {
        return "/projects/" + projectId + "/use-case-points";
    }

    @Override
    public String getAddPath(Long projectId) {
        return "/projects/" + projectId + "/use-case-points/add";
    }
}