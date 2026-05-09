package com.uniovi.estimacion.controllers.effortconversions.transformationfunctions;

import com.uniovi.estimacion.entities.sizeanalyses.SizeAnalysis;
import com.uniovi.estimacion.entities.effortconversions.transformationfunctions.TransformationFunction;
import com.uniovi.estimacion.entities.effortconversions.transformationfunctions.TransformationFunctionConversion;
import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.services.sizeanalyses.SizeAnalysisProvider;
import com.uniovi.estimacion.services.sizeanalyses.SizeAnalysisProviderRegistry;
import com.uniovi.estimacion.services.costs.CostCalculationService;
import com.uniovi.estimacion.services.effortconversions.transformationfunctions.TransformationFunctionService;
import com.uniovi.estimacion.services.projects.EstimationProjectService;
import com.uniovi.estimacion.validators.effortconversions.transformationfunctions.TransformationFunctionValidator;
import com.uniovi.estimacion.web.forms.effortconversions.transformationfunctions.TransformationFunctionConversionForm;
import com.uniovi.estimacion.web.forms.effortconversions.transformationfunctions.TransformationFunctionForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/projects/{projectId}/size-analyses/{sourceTechniqueCode}/transformation-functions")
@RequiredArgsConstructor
public class TransformationFunctionController {

    private final EstimationProjectService estimationProjectService;
    private final SizeAnalysisProviderRegistry sizeAnalysisProviderRegistry;
    private final TransformationFunctionService transformationFunctionService;
    private final TransformationFunctionValidator transformationFunctionValidator;
    private final CostCalculationService costCalculationService;

    @GetMapping("/access")
    public String accessTransformationFunction(@PathVariable Long projectId,
                                               @PathVariable String sourceTechniqueCode) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<? extends SizeAnalysis> optionalAnalysis =
                findSourceAnalysis(projectId, sourceTechniqueCode);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToSourceAnalysisAdd(projectId, sourceTechniqueCode);
        }

        Optional<TransformationFunctionConversion> optionalActiveConversion =
                transformationFunctionService.findActiveBySourceAnalysis(optionalAnalysis.get());

        if (optionalActiveConversion.isPresent()) {
            return redirectToDetails(projectId, sourceTechniqueCode, optionalActiveConversion.get().getId());
        }

        return redirectToSelect(projectId, sourceTechniqueCode);
    }

    @GetMapping("/select")
    public String getSelectForm(@PathVariable Long projectId,
                                @PathVariable String sourceTechniqueCode,
                                Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<? extends SizeAnalysis> optionalAnalysis =
                findSourceAnalysis(projectId, sourceTechniqueCode);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToSourceAnalysisAdd(projectId, sourceTechniqueCode);
        }

        loadSelectModel(
                optionalProject.get(),
                optionalAnalysis.get(),
                sourceTechniqueCode,
                new TransformationFunctionConversionForm(),
                new TransformationFunctionForm(),
                model
        );

        return "effortconversions/transformationfunctions/select";
    }

    @PostMapping("/select")
    public String createConversion(@PathVariable Long projectId,
                                   @PathVariable String sourceTechniqueCode,
                                   @ModelAttribute("conversionForm") TransformationFunctionConversionForm conversionForm,
                                   BindingResult result,
                                   Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<? extends SizeAnalysis> optionalAnalysis =
                findSourceAnalysis(projectId, sourceTechniqueCode);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToSourceAnalysisAdd(projectId, sourceTechniqueCode);
        }

        EstimationProject project = optionalProject.get();
        SizeAnalysis analysis = optionalAnalysis.get();

        if (conversionForm.getTransformationFunctionId() == null) {
            result.rejectValue("transformationFunctionId", "transformationFunctionConversion.validation.function.empty");
        }

        if (result.hasErrors()) {
            loadSelectModel(
                    project,
                    analysis,
                    sourceTechniqueCode,
                    conversionForm,
                    new TransformationFunctionForm(),
                    model
            );
            return "effortconversions/transformationfunctions/select";
        }

        TransformationFunctionConversion conversion =
                transformationFunctionService.createConversion(
                        analysis,
                        conversionForm.getTransformationFunctionId()
                );

        return redirectToDetails(projectId, sourceTechniqueCode, conversion.getId());
    }

    @PostMapping("/functions/add")
    public String createCustomFunction(@PathVariable Long projectId,
                                       @PathVariable String sourceTechniqueCode,
                                       @ModelAttribute("functionForm") TransformationFunctionForm functionForm,
                                       BindingResult result,
                                       Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<? extends SizeAnalysis> optionalAnalysis =
                findSourceAnalysis(projectId, sourceTechniqueCode);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToSourceAnalysisAdd(projectId, sourceTechniqueCode);
        }

        EstimationProject project = optionalProject.get();
        SizeAnalysis analysis = optionalAnalysis.get();

        transformationFunctionValidator.validate(functionForm, result);

        if (result.hasErrors()) {
            loadSelectModel(
                    project,
                    analysis,
                    sourceTechniqueCode,
                    new TransformationFunctionConversionForm(),
                    functionForm,
                    model
            );
            return "effortconversions/transformationfunctions/select";
        }

        TransformationFunction function =
                transformationFunctionService.createCustomFunction(
                        analysis,
                        functionForm.getName(),
                        functionForm.getDescription(),
                        functionForm.getIntercept(),
                        functionForm.getSlope()
                );

        TransformationFunctionConversion conversion =
                transformationFunctionService.createConversion(analysis, function.getId());

        return redirectToDetails(projectId, sourceTechniqueCode, conversion.getId());
    }

    @GetMapping("/{conversionId}")
    public String getDetails(@PathVariable Long projectId,
                             @PathVariable String sourceTechniqueCode,
                             @PathVariable Long conversionId,
                             Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<? extends SizeAnalysis> optionalAnalysis =
                findSourceAnalysis(projectId, sourceTechniqueCode);
        Optional<TransformationFunctionConversion> optionalConversion =
                transformationFunctionService.findByIdAndProjectId(conversionId, projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToSourceAnalysisAdd(projectId, sourceTechniqueCode);
        }

        if (optionalConversion.isEmpty()
                || !belongsToSourceAnalysis(optionalConversion.get(), optionalAnalysis.get())) {
            return redirectToSourceAnalysisDetails(projectId, sourceTechniqueCode);
        }

        EstimationProject project = optionalProject.get();
        SizeAnalysis analysis = optionalAnalysis.get();
        TransformationFunctionConversion conversion = optionalConversion.get();

        Double currentSize = analysis.getCalculatedSizeValue();
        Double estimatedEffortHours =
                transformationFunctionService.calculateEstimatedEffortHours(conversion, currentSize);

        BigDecimal estimatedCost =
                costCalculationService.calculateCost(
                        estimatedEffortHours,
                        project.getHourlyRate()
                );

        model.addAttribute("project", project);
        model.addAttribute("analysis", analysis);
        model.addAttribute("conversion", conversion);
        model.addAttribute("sourceTechniqueCode", sourceTechniqueCode);
        model.addAttribute("sourceAnalysisDetailsPath",
                getSourceAnalysisProvider(sourceTechniqueCode).getDetailsPath(projectId));
        model.addAttribute("currentSize", currentSize);
        model.addAttribute("estimatedEffortHours", estimatedEffortHours);
        model.addAttribute("estimatedCost", estimatedCost);

        return "effortconversions/transformationfunctions/details";
    }

    @GetMapping("/{conversionId}/delete")
    public String deleteConversion(@PathVariable Long projectId,
                                   @PathVariable String sourceTechniqueCode,
                                   @PathVariable Long conversionId) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<? extends SizeAnalysis> optionalAnalysis =
                findSourceAnalysis(projectId, sourceTechniqueCode);
        Optional<TransformationFunctionConversion> optionalConversion =
                transformationFunctionService.findByIdAndProjectId(conversionId, projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isPresent()
                && optionalConversion.isPresent()
                && belongsToSourceAnalysis(optionalConversion.get(), optionalAnalysis.get())) {
            transformationFunctionService.deleteByIdAndProjectId(conversionId, projectId);
        }

        return redirectToSourceAnalysisDetails(projectId, sourceTechniqueCode);
    }

    private void loadSelectModel(EstimationProject project,
                                 SizeAnalysis analysis,
                                 String sourceTechniqueCode,
                                 TransformationFunctionConversionForm conversionForm,
                                 TransformationFunctionForm functionForm,
                                 Model model) {
        List<TransformationFunction> availableFunctions =
                transformationFunctionService.findAvailableFunctions(analysis);

        model.addAttribute("project", project);
        model.addAttribute("analysis", analysis);
        model.addAttribute("sourceTechniqueCode", sourceTechniqueCode);
        model.addAttribute("sourceAnalysisDetailsPath",
                getSourceAnalysisProvider(sourceTechniqueCode).getDetailsPath(project.getId()));
        model.addAttribute("availableFunctions", availableFunctions);
        model.addAttribute("conversionForm", conversionForm);
        model.addAttribute("functionForm", functionForm);
    }

    private boolean belongsToSourceAnalysis(TransformationFunctionConversion conversion, SizeAnalysis analysis) {
        return conversion != null
                && analysis != null
                && conversion.getSourceAnalysisId() != null
                && conversion.getSourceTechniqueCode() != null
                && conversion.getSourceAnalysisId().equals(analysis.getId())
                && conversion.getSourceTechniqueCode().equals(analysis.getTechniqueCode());
    }

    private SizeAnalysisProvider getSourceAnalysisProvider(String sourceTechniqueCode) {
        return sizeAnalysisProviderRegistry.getByTechniqueCode(sourceTechniqueCode);
    }

    private Optional<? extends SizeAnalysis> findSourceAnalysis(Long projectId,
                                                                String sourceTechniqueCode) {
        return getSourceAnalysisProvider(sourceTechniqueCode).findDetailedByProjectId(projectId);
    }

    private String redirectToProjects() {
        return "redirect:/projects";
    }

    private String redirectToSourceAnalysisAdd(Long projectId, String sourceTechniqueCode) {
        return "redirect:" + getSourceAnalysisProvider(sourceTechniqueCode).getAddPath(projectId);
    }

    private String redirectToSourceAnalysisDetails(Long projectId, String sourceTechniqueCode) {
        return "redirect:" + getSourceAnalysisProvider(sourceTechniqueCode).getDetailsPath(projectId);
    }

    private String redirectToSelect(Long projectId, String sourceTechniqueCode) {
        return "redirect:/projects/" + projectId
                + "/size-analyses/" + sourceTechniqueCode
                + "/transformation-functions/select";
    }

    private String redirectToDetails(Long projectId,
                                     String sourceTechniqueCode,
                                     Long conversionId) {
        return "redirect:/projects/" + projectId
                + "/size-analyses/" + sourceTechniqueCode
                + "/transformation-functions/" + conversionId;
    }
}