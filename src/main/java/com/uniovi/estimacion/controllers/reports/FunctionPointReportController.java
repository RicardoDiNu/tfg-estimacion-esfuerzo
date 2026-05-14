package com.uniovi.estimacion.controllers.reports;

import com.uniovi.estimacion.entities.effortconversions.delphi.DelphiEstimation;
import com.uniovi.estimacion.entities.effortconversions.transformationfunctions.TransformationFunctionConversion;
import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.FunctionPointAnalysis;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.DataFunction;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.TransactionalFunction;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.modules.FunctionPointModule;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.requirements.UserRequirement;
import com.uniovi.estimacion.services.costs.CostCalculationService;
import com.uniovi.estimacion.services.effortconversions.delphi.DelphiEstimationService;
import com.uniovi.estimacion.services.effortconversions.transformationfunctions.TransformationFunctionService;
import com.uniovi.estimacion.services.projects.EstimationProjectService;
import com.uniovi.estimacion.services.reports.PdfReportRendererService;
import com.uniovi.estimacion.services.sizeanalyses.functionpoints.*;
import com.uniovi.estimacion.web.dtos.reports.functionpoints.FunctionPointModuleReportRow;
import com.uniovi.estimacion.web.forms.sizeanalyses.functionpoints.FunctionPointWeightMatrixForm;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/projects/{projectId}/function-points/report")
@RequiredArgsConstructor
public class FunctionPointReportController {

    private final EstimationProjectService estimationProjectService;
    private final FunctionPointAnalysisService functionPointAnalysisService;
    private final FunctionPointCalculationService functionPointCalculationService;
    private final PdfReportRendererService pdfReportRendererService;
    private final FunctionPointModuleService functionPointModuleService;
    private final UserRequirementService userRequirementService;
    private final DelphiEstimationService delphiEstimationService;
    private final TransformationFunctionService transformationFunctionService;
    private final CostCalculationService costCalculationService;

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> generatePdfReport(@PathVariable Long projectId) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);

        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisService.findDetailedByProjectId(projectId);

        if (optionalProject.isEmpty() || optionalAnalysis.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        EstimationProject project = optionalProject.get();
        FunctionPointAnalysis analysis = optionalAnalysis.get();

        FunctionPointAnalysisSummary results =
                functionPointCalculationService.buildSummary(analysis);

        FunctionPointWeightMatrixForm weightMatrixForm =
                functionPointAnalysisService.buildWeightMatrixForm(projectId)
                        .orElse(null);

        List<FunctionPointModuleReportRow> moduleRows = buildModuleRows(projectId, analysis);


        Map<Long, Double> moduleSizeById = new LinkedHashMap<>();

        for (FunctionPointModuleReportRow moduleRow : moduleRows) {
            if (moduleRow.getModule() != null
                    && moduleRow.getModule().getId() != null
                    && moduleRow.getResults() != null) {

                moduleSizeById.put(
                        moduleRow.getModule().getId(),
                        moduleRow.getResults().getAdjustedFunctionPoints()
                );
            }
        }

        Optional<DelphiEstimation> optionalActiveDelphi =
                delphiEstimationService.findDetailedActiveBySourceAnalysis(analysis);

        Double delphiEstimatedTotalHours = null;
        Integer activeDelphiIterationsCount = 0;
        BigDecimal delphiEstimatedCost = null;

        if (optionalActiveDelphi.isPresent()) {
            DelphiEstimation activeDelphi = optionalActiveDelphi.get();
            activeDelphiIterationsCount = activeDelphi.getIterations().size();

            if (activeDelphi.getRegressionIntercept() != null
                    && activeDelphi.getRegressionSlope() != null) {

                delphiEstimatedTotalHours =
                        delphiEstimationService.calculateTotalEstimatedEffortHours(
                                activeDelphi,
                                moduleSizeById
                        );

                delphiEstimatedCost =
                        costCalculationService.calculateCost(
                                delphiEstimatedTotalHours,
                                project.getHourlyRate()
                        );
            }
        }

        Optional<TransformationFunctionConversion> optionalActiveTransformationConversion =
                transformationFunctionService.findActiveBySourceAnalysis(analysis);

        TransformationFunctionConversion activeTransformationConversion = null;
        Double transformationEstimatedHours = null;
        BigDecimal transformationEstimatedCost = null;

        if (optionalActiveTransformationConversion.isPresent()) {
            activeTransformationConversion = optionalActiveTransformationConversion.get();

            transformationEstimatedHours =
                    transformationFunctionService.calculateEstimatedEffortHours(
                            activeTransformationConversion,
                            analysis.getCalculatedSizeValue()
                    );

            transformationEstimatedCost =
                    costCalculationService.calculateCost(
                            transformationEstimatedHours,
                            project.getHourlyRate()
                    );
        }

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("project", project);
        model.put("analysis", analysis);
        model.put("results", results);
        model.put("weightMatrixForm", weightMatrixForm);
        model.put("moduleRows", moduleRows);
        model.put("activeDelphiEstimation", optionalActiveDelphi.orElse(null));
        model.put("activeDelphiIterationsCount", activeDelphiIterationsCount);
        model.put("delphiEstimatedTotalHours", delphiEstimatedTotalHours);
        model.put("delphiEstimatedCost", delphiEstimatedCost);

        model.put("activeTransformationConversion", activeTransformationConversion);
        model.put("transformationEstimatedHours", transformationEstimatedHours);
        model.put("transformationEstimatedCost", transformationEstimatedCost);
        model.put("generatedAt", LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        byte[] pdfBytes = pdfReportRendererService.renderToPdf(
                "reports/functionpoints/report",
                model
        );

        String filename = "informe-pf-proyecto-" + projectId + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    private List<FunctionPointModuleReportRow> buildModuleRows(Long projectId,
                                                               FunctionPointAnalysis analysis) {
        List<FunctionPointModule> modules =
                functionPointModuleService.findAllByProjectId(projectId);

        List<FunctionPointModuleReportRow> rows = new ArrayList<>();

        for (FunctionPointModule module : modules) {
            List<DataFunction> dataFunctions =
                    functionPointAnalysisService.findAllDataFunctionsByModuleId(module.getId());

            List<TransactionalFunction> transactionalFunctions =
                    functionPointAnalysisService.findAllTransactionalFunctionsByModuleId(module.getId());

            FunctionPointAnalysisSummary moduleResults =
                    functionPointCalculationService.buildModuleSummary(
                            analysis,
                            dataFunctions,
                            transactionalFunctions
                    );

            List<UserRequirement> requirements =
                    userRequirementService.findDetailedAllByModuleId(module.getId());

            rows.add(new FunctionPointModuleReportRow(
                    module,
                    moduleResults,
                    dataFunctions,
                    transactionalFunctions,
                    requirements
            ));
        }

        return rows;
    }
}