package com.uniovi.estimacion.controllers.reports;

import com.uniovi.estimacion.entities.effortconversions.delphi.DelphiEstimation;
import com.uniovi.estimacion.entities.effortconversions.transformationfunctions.TransformationFunctionConversion;
import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.UseCasePointAnalysis;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.modules.UseCasePointModule;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.usecases.UseCaseEntry;
import com.uniovi.estimacion.services.costs.CostCalculationService;
import com.uniovi.estimacion.services.effortconversions.delphi.DelphiEstimationService;
import com.uniovi.estimacion.services.effortconversions.transformationfunctions.TransformationFunctionService;
import com.uniovi.estimacion.services.projects.EstimationProjectService;
import com.uniovi.estimacion.services.reports.PdfReportRendererService;
import com.uniovi.estimacion.services.sizeanalyses.SizeAnalysisModuleResult;
import com.uniovi.estimacion.services.sizeanalyses.usecasepoints.UseCasePointAnalysisService;
import com.uniovi.estimacion.services.sizeanalyses.usecasepoints.UseCasePointAnalysisSummary;
import com.uniovi.estimacion.services.sizeanalyses.usecasepoints.UseCasePointCalculationService;
import com.uniovi.estimacion.services.sizeanalyses.usecasepoints.UseCasePointSizeAnalysisProvider;
import com.uniovi.estimacion.web.dtos.reports.usecasepoints.UseCasePointModuleReportRow;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/projects/{projectId}/use-case-points/report")
@RequiredArgsConstructor
public class UseCasePointReportController {

    private final EstimationProjectService estimationProjectService;
    private final UseCasePointAnalysisService useCasePointAnalysisService;
    private final UseCasePointCalculationService useCasePointCalculationService;
    private final UseCasePointSizeAnalysisProvider useCasePointSizeAnalysisProvider;
    private final PdfReportRendererService pdfReportRendererService;

    private final DelphiEstimationService delphiEstimationService;
    private final TransformationFunctionService transformationFunctionService;
    private final CostCalculationService costCalculationService;

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> generatePdfReport(@PathVariable Long projectId) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);

        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisService.findDetailedByProjectId(projectId);

        if (optionalProject.isEmpty() || optionalAnalysis.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        EstimationProject project = optionalProject.get();
        UseCasePointAnalysis analysis = optionalAnalysis.get();

        UseCasePointAnalysisSummary results =
                useCasePointCalculationService.buildSummary(analysis);

        List<UseCasePointModuleReportRow> moduleRows =
                buildModuleRows(projectId, analysis);

        Optional<DelphiEstimation> optionalActiveDelphi =
                delphiEstimationService.findDetailedActiveBySourceAnalysis(analysis);

        DelphiEstimation activeUseCasePointDelphi = null;
        Integer activeUseCasePointDelphiIterationsCount = 0;
        Double useCasePointDelphiEstimatedTotalHours = null;
        BigDecimal useCasePointDelphiEstimatedCost = null;

        if (optionalActiveDelphi.isPresent()) {
            activeUseCasePointDelphi = optionalActiveDelphi.get();
            activeUseCasePointDelphiIterationsCount =
                    activeUseCasePointDelphi.getIterations() != null
                            ? activeUseCasePointDelphi.getIterations().size()
                            : 0;

            if (activeUseCasePointDelphi.getRegressionIntercept() != null
                    && activeUseCasePointDelphi.getRegressionSlope() != null) {

                List<SizeAnalysisModuleResult> moduleResults =
                        useCasePointSizeAnalysisProvider.buildModuleResults(analysis);

                useCasePointDelphiEstimatedTotalHours =
                        delphiEstimationService.calculateTotalEstimatedEffortHours(
                                activeUseCasePointDelphi,
                                moduleResults
                        );

                useCasePointDelphiEstimatedCost =
                        costCalculationService.calculateCost(
                                useCasePointDelphiEstimatedTotalHours,
                                project.getHourlyRate()
                        );
            }
        }

        Optional<TransformationFunctionConversion> optionalActiveTransformationConversion =
                transformationFunctionService.findActiveBySourceAnalysis(analysis);

        TransformationFunctionConversion activeUseCasePointTransformationConversion = null;
        Double useCasePointTransformationEstimatedHours = null;
        BigDecimal useCasePointTransformationEstimatedCost = null;

        if (optionalActiveTransformationConversion.isPresent()) {
            activeUseCasePointTransformationConversion =
                    optionalActiveTransformationConversion.get();

            useCasePointTransformationEstimatedHours =
                    transformationFunctionService.calculateEstimatedEffortHours(
                            activeUseCasePointTransformationConversion,
                            analysis.getCalculatedSizeValue()
                    );

            useCasePointTransformationEstimatedCost =
                    costCalculationService.calculateCost(
                            useCasePointTransformationEstimatedHours,
                            project.getHourlyRate()
                    );
        }

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("project", project);
        model.put("analysis", analysis);
        model.put("results", results);
        model.put("moduleRows", moduleRows);

        model.put("actors", analysis.getActors());
        model.put("technicalFactors", analysis.getTechnicalFactorAssessments());
        model.put("environmentalFactors", analysis.getEnvironmentalFactorAssessments());

        model.put("activeUseCasePointDelphi", activeUseCasePointDelphi);
        model.put("activeUseCasePointDelphiIterationsCount", activeUseCasePointDelphiIterationsCount);
        model.put("useCasePointDelphiEstimatedTotalHours", useCasePointDelphiEstimatedTotalHours);
        model.put("useCasePointDelphiEstimatedCost", useCasePointDelphiEstimatedCost);

        model.put("activeUseCasePointTransformationConversion", activeUseCasePointTransformationConversion);
        model.put("useCasePointTransformationEstimatedHours", useCasePointTransformationEstimatedHours);
        model.put("useCasePointTransformationEstimatedCost", useCasePointTransformationEstimatedCost);

        model.put("generatedAt", LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        byte[] pdfBytes = pdfReportRendererService.renderToPdf(
                "reports/usecasepoints/report",
                model
        );

        String filename = "informe-ucp-proyecto-" + projectId + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    private List<UseCasePointModuleReportRow> buildModuleRows(Long projectId,
                                                              UseCasePointAnalysis analysis) {
        List<UseCasePointModule> modules =
                useCasePointAnalysisService.findAllModulesByProjectId(projectId);

        List<UseCasePointModuleReportRow> rows = new ArrayList<>();

        for (UseCasePointModule module : modules) {
            List<UseCaseEntry> useCases =
                    useCasePointAnalysisService.findAllUseCasesByModuleId(module.getId());

            Integer useCaseWeight = useCases.stream()
                    .mapToInt(useCase -> useCase.getWeight() != null ? useCase.getWeight() : 0)
                    .sum();

            Double unadjustedUseCasePoints =
                    useCasePointCalculationService.calculateUnadjustedUseCasePointsForModule(
                            analysis,
                            useCases
                    );

            Double adjustedUseCasePoints =
                    useCasePointCalculationService.calculateAdjustedUseCasePointsForModule(
                            analysis,
                            useCases
                    );

            rows.add(new UseCasePointModuleReportRow(
                    module,
                    useCases,
                    useCaseWeight,
                    unadjustedUseCasePoints,
                    adjustedUseCasePoints
            ));
        }

        return rows;
    }
}