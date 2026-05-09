package com.uniovi.estimacion.controllers.sizeanalyses.functionpoints;

import com.uniovi.estimacion.entities.effortconversions.delphi.DelphiEstimation;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.FunctionPointAnalysis;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.DataFunction;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.TransactionalFunction;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.modules.FunctionPointModule;
import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.requirements.UserRequirement;
import com.uniovi.estimacion.services.costs.CostCalculationService;
import com.uniovi.estimacion.services.effortconversions.delphi.DelphiEstimationService;
import com.uniovi.estimacion.services.effortconversions.transformationfunctions.TransformationFunctionService;
import com.uniovi.estimacion.services.projects.ProjectAuthorizationService;
import com.uniovi.estimacion.services.sizeanalyses.functionpoints.FunctionPointAnalysisService;
import com.uniovi.estimacion.services.sizeanalyses.functionpoints.FunctionPointAnalysisSummary;
import com.uniovi.estimacion.services.sizeanalyses.functionpoints.FunctionPointCalculationService;
import com.uniovi.estimacion.services.sizeanalyses.functionpoints.FunctionPointModuleService;
import com.uniovi.estimacion.services.projects.EstimationProjectService;
import com.uniovi.estimacion.services.sizeanalyses.functionpoints.UserRequirementService;
import com.uniovi.estimacion.validators.sizeanalyses.functionpoints.FunctionPointAnalysisValidator;
import com.uniovi.estimacion.validators.sizeanalyses.functionpoints.GeneralSystemCharacteristicsValidator;
import com.uniovi.estimacion.entities.effortconversions.transformationfunctions.TransformationFunctionConversion;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/projects/{projectId}")
@RequiredArgsConstructor
public class FunctionPointAnalysisController {

    private final EstimationProjectService estimationProjectService;
    private final FunctionPointAnalysisService functionPointAnalysisService;
    private final UserRequirementService userRequirementService;
    private final FunctionPointAnalysisValidator functionPointAnalysisValidator;
    private final GeneralSystemCharacteristicsValidator generalSystemCharacteristicsValidator;
    private final FunctionPointCalculationService functionPointCalculationService;
    private final FunctionPointModuleService functionPointModuleService;
    private final DelphiEstimationService delphiEstimationService;
    private final TransformationFunctionService transformationFunctionService;
    private final CostCalculationService costCalculationService;
    private final ProjectAuthorizationService projectAuthorizationService;

    @GetMapping("/function-points/add")
    public String getCreateForm(@PathVariable Long projectId, Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (!projectAuthorizationService.canManageProject(projectId)) {
            return redirectToFunctionPointDetails(projectId);
        }

        if (functionPointAnalysisService.findByProjectId(projectId).isPresent()) {
            return redirectToFunctionPointDetails(projectId);
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("analysis", new FunctionPointAnalysis());
        return "fp/add";
    }

    @PostMapping("/function-points/add")
    public String createAnalysis(@PathVariable Long projectId,
                                 @ModelAttribute("analysis") FunctionPointAnalysis analysis,
                                 BindingResult result,
                                 Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (!projectAuthorizationService.canManageProject(projectId)) {
            return redirectToFunctionPointDetails(projectId);
        }

        if (functionPointAnalysisService.findByProjectId(projectId).isPresent()) {
            return redirectToFunctionPointDetails(projectId);
        }

        functionPointAnalysisValidator.validate(analysis, result);

        if (result.hasErrors()) {
            model.addAttribute("project", optionalProject.get());
            model.addAttribute("analysis", analysis);
            return "fp/add";
        }

        functionPointAnalysisService.createInitialAnalysis(
                optionalProject.get(),
                analysis.getSystemBoundaryDescription()
        );

        return redirectToFunctionPointDetails(projectId);
    }

    @GetMapping("/function-points")
    public String getFunctionPointAnalysisDetails(@PathVariable Long projectId,
                                                  @RequestParam(name = "modulesPage", defaultValue = "0") int modulesPage,
                                                  @RequestParam(name = "requirementsPage", defaultValue = "0") int requirementsPage,
                                                  @RequestParam(name = "dataFunctionsPage", defaultValue = "0") int dataFunctionsPage,
                                                  @RequestParam(name = "transactionalFunctionsPage", defaultValue = "0") int transactionalFunctionsPage,
                                                  Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisService.findDetailedByProjectId(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToFunctionPointAdd(projectId);
        }

        EstimationProject project = optionalProject.get();
        FunctionPointAnalysis analysis = optionalAnalysis.get();

        FunctionPointAnalysisSummary results = functionPointCalculationService.buildSummary(analysis);

        List<FunctionPointModule> allModules =
                functionPointModuleService.findAllByProjectId(projectId);

        Page<FunctionPointModule> modulesPageResult =
                functionPointModuleService.findPageByProjectId(projectId, PageRequest.of(modulesPage, 5));

        Map<Long, FunctionPointAnalysisSummary> moduleResultsMap = new LinkedHashMap<>();
        Map<Long, Double> moduleSizeById = new LinkedHashMap<>();

        for (FunctionPointModule module : allModules) {
            List<DataFunction> moduleDataFunctions =
                    functionPointAnalysisService.findAllDataFunctionsByModuleId(module.getId());

            List<TransactionalFunction> moduleTransactionalFunctions =
                    functionPointAnalysisService.findAllTransactionalFunctionsByModuleId(module.getId());

            FunctionPointAnalysisSummary moduleSummary =
                    functionPointCalculationService.buildModuleSummary(
                            analysis,
                            moduleDataFunctions,
                            moduleTransactionalFunctions
                    );

            moduleSizeById.put(module.getId(), moduleSummary.getAdjustedFunctionPoints());

            if (modulesPageResult.getContent().stream().anyMatch(m -> m.getId().equals(module.getId()))) {
                moduleResultsMap.put(module.getId(), moduleSummary);
            }
        }

        Optional<DelphiEstimation> optionalActiveDelphi =
                delphiEstimationService.findDetailedActiveBySourceAnalysis(analysis);

        Double delphiEstimatedTotalHours = null;
        Integer activeDelphiIterationsCount = 0;
        BigDecimal delphiEstimatedCost = null;
        BigDecimal transformationEstimatedCost = null;

        Map<Long, Double> moduleDelphiEffortById = new LinkedHashMap<>();

        if (optionalActiveDelphi.isPresent()) {
            DelphiEstimation activeDelphi = optionalActiveDelphi.get();
            activeDelphiIterationsCount = activeDelphi.getIterations().size();

            if (activeDelphi.getRegressionIntercept() != null
                    && activeDelphi.getRegressionSlope() != null) {

                for (Map.Entry<Long, Double> entry : moduleSizeById.entrySet()) {
                    Double moduleSize = entry.getValue();

                    if (moduleSize != null && moduleSize > 0) {
                        moduleDelphiEffortById.put(
                                entry.getKey(),
                                delphiEstimationService.calculateEstimatedEffortHours(activeDelphi, moduleSize)
                        );
                    }
                }

                delphiEstimatedTotalHours =
                        delphiEstimationService.calculateTotalEstimatedEffortHours(activeDelphi, moduleSizeById);
                delphiEstimatedCost =
                        costCalculationService.calculateCost(
                                delphiEstimatedTotalHours,
                                project.getHourlyRate()
                        );
            }
        }

        Page<UserRequirement> requirementsPageResult =
                userRequirementService.findPageByProjectId(projectId, PageRequest.of(requirementsPage, 5));

        Page<DataFunction> dataFunctionsPageResult =
                functionPointAnalysisService.findDataFunctionsPageByProjectId(projectId, PageRequest.of(dataFunctionsPage, 5));

        Page<TransactionalFunction> transactionalFunctionsPageResult =
                functionPointAnalysisService.findTransactionalFunctionsPageByProjectId(projectId, PageRequest.of(transactionalFunctionsPage, 5));

        Optional<TransformationFunctionConversion> optionalActiveTransformationConversion =
                transformationFunctionService.findActiveBySourceAnalysis(analysis);

        TransformationFunctionConversion activeTransformationConversion = null;
        Double transformationEstimatedHours = null;

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

        model.addAttribute("activeTransformationConversion", activeTransformationConversion);
        model.addAttribute("transformationEstimatedHours", transformationEstimatedHours);
        model.addAttribute("project", project);
        model.addAttribute("analysis", analysis);
        model.addAttribute("sourceTechniqueCode", analysis.getTechniqueCode());
        model.addAttribute("results", results);
        model.addAttribute("modulesList", modulesPageResult.getContent());
        model.addAttribute("modulesPage", modulesPageResult);
        model.addAttribute("moduleResultsMap", moduleResultsMap);

        model.addAttribute("canManageProject",
                projectAuthorizationService.canManageProject(projectId));

        model.addAttribute("canEditEstimationData",
                projectAuthorizationService.canEditEstimationData(projectId));

        model.addAttribute("canManageEffortConversions",
                projectAuthorizationService.canManageEffortConversions(projectId));

        model.addAttribute("activeDelphiEstimation", optionalActiveDelphi.orElse(null));
        model.addAttribute("activeDelphiIterationsCount", activeDelphiIterationsCount);
        model.addAttribute("delphiEstimatedTotalHours", delphiEstimatedTotalHours);
        model.addAttribute("moduleDelphiEffortById", moduleDelphiEffortById);

        model.addAttribute("delphiEstimatedCost", delphiEstimatedCost);
        model.addAttribute("transformationEstimatedCost", transformationEstimatedCost);

        model.addAttribute("requirementsList", requirementsPageResult.getContent());
        model.addAttribute("requirementsPage", requirementsPageResult);

        model.addAttribute("dataFunctionsList", dataFunctionsPageResult.getContent());
        model.addAttribute("dataFunctionsPage", dataFunctionsPageResult);

        model.addAttribute("transactionalFunctionsList", transactionalFunctionsPageResult.getContent());
        model.addAttribute("transactionalFunctionsPage", transactionalFunctionsPageResult);

        model.addAttribute("dataFunctionsCurrentPage", dataFunctionsPageResult.getNumber());
        model.addAttribute("transactionalFunctionsCurrentPage", transactionalFunctionsPageResult.getNumber());

        return "fp/details";
    }

    @GetMapping("/function-points/modules/update")
    public String updateModulesSection(@PathVariable Long projectId,
                                       @RequestParam(name = "modulesPage", defaultValue = "0") int modulesPage,
                                       Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisService.findDetailedByProjectId(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToFunctionPointAdd(projectId);
        }

        FunctionPointAnalysis analysis = optionalAnalysis.get();

        List<FunctionPointModule> allModules =
                functionPointModuleService.findAllByProjectId(projectId);

        Page<FunctionPointModule> modulesPageResult =
                functionPointModuleService.findPageByProjectId(projectId, PageRequest.of(modulesPage, 5));

        Map<Long, FunctionPointAnalysisSummary> moduleResultsMap = new LinkedHashMap<>();

        for (FunctionPointModule module : modulesPageResult.getContent()) {
            List<DataFunction> moduleDataFunctions =
                    functionPointAnalysisService.findAllDataFunctionsByModuleId(module.getId());

            List<TransactionalFunction> moduleTransactionalFunctions =
                    functionPointAnalysisService.findAllTransactionalFunctionsByModuleId(module.getId());

            FunctionPointAnalysisSummary moduleSummary =
                    functionPointCalculationService.buildModuleSummary(
                            analysis,
                            moduleDataFunctions,
                            moduleTransactionalFunctions
                    );

            moduleResultsMap.put(module.getId(), moduleSummary);
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("modulesList", modulesPageResult.getContent());
        model.addAttribute("modulesPage", modulesPageResult);
        model.addAttribute("moduleResultsMap", moduleResultsMap);

        model.addAttribute("canManageProject",
                projectAuthorizationService.canManageProject(projectId));

        model.addAttribute("canEditEstimationData",
                projectAuthorizationService.canEditEstimationData(projectId));

        model.addAttribute("canManageEffortConversions",
                projectAuthorizationService.canManageEffortConversions(projectId));

        return "fp/details :: modulesSection";
    }

    @GetMapping("/function-points/edit")
    public String getEditForm(@PathVariable Long projectId, Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisService.findByProjectId(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (!projectAuthorizationService.canEditEstimationData(projectId)) {
            return redirectToFunctionPointDetails(projectId);
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToFunctionPointAdd(projectId);
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("analysis", optionalAnalysis.get());
        model.addAttribute("systemBoundaryDescription", optionalAnalysis.get().getSystemBoundaryDescription());

        return "fp/edit";
    }

    @PostMapping("/function-points/edit")
    public String updateAnalysis(@PathVariable Long projectId,
                                 @ModelAttribute("analysis") FunctionPointAnalysis formAnalysis,
                                 BindingResult result,
                                 Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisService.findByProjectId(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (!projectAuthorizationService.canEditEstimationData(projectId)) {
            return redirectToFunctionPointDetails(projectId);
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToFunctionPointAdd(projectId);
        }

        functionPointAnalysisValidator.validate(formAnalysis, result);

        if (result.hasErrors()) {
            model.addAttribute("project", optionalProject.get());
            model.addAttribute("analysis", formAnalysis);
            return "fp/edit";
        }

        boolean updated = functionPointAnalysisService
                .updateSystemBoundaryDescription(projectId, formAnalysis.getSystemBoundaryDescription());

        if (!updated) {
            model.addAttribute("project", optionalProject.get());
            model.addAttribute("analysis", formAnalysis);
            model.addAttribute("error", "El límite del sistema no puede estar vacío.");
            return "fp/edit";
        }

        return redirectToFunctionPointDetails(projectId);
    }

    @GetMapping("/function-points/requirements/update")
    public String updateRequirementsSection(@PathVariable Long projectId,
                                            @RequestParam(name = "requirementsPage", defaultValue = "0") int requirementsPage,
                                            @RequestParam(name = "dataFunctionsPage", defaultValue = "0") int dataFunctionsPage,
                                            @RequestParam(name = "transactionalFunctionsPage", defaultValue = "0") int transactionalFunctionsPage,
                                            Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisService.findDetailedByProjectId(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToFunctionPointAdd(projectId);
        }

        Page<UserRequirement> requirementsPageResult =
                userRequirementService.findPageByProjectId(projectId, PageRequest.of(requirementsPage, 5));

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("requirementsList", requirementsPageResult.getContent());
        model.addAttribute("requirementsPage", requirementsPageResult);
        model.addAttribute("dataFunctionsCurrentPage", dataFunctionsPage);
        model.addAttribute("transactionalFunctionsCurrentPage", transactionalFunctionsPage);

        model.addAttribute("canManageProject",
                projectAuthorizationService.canManageProject(projectId));

        model.addAttribute("canEditEstimationData",
                projectAuthorizationService.canEditEstimationData(projectId));

        model.addAttribute("canManageEffortConversions",
                projectAuthorizationService.canManageEffortConversions(projectId));

        return "fp/details :: requirementsSection";
    }

    @GetMapping("/function-points/data-functions/update")
    public String updateDataFunctionsSection(@PathVariable Long projectId,
                                             @RequestParam(name = "requirementsPage", defaultValue = "0") int requirementsPage,
                                             @RequestParam(name = "dataFunctionsPage", defaultValue = "0") int dataFunctionsPage,
                                             @RequestParam(name = "transactionalFunctionsPage", defaultValue = "0") int transactionalFunctionsPage,
                                             Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisService.findDetailedByProjectId(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToFunctionPointAdd(projectId);
        }

        Page<DataFunction> dataFunctionsPageResult =
                functionPointAnalysisService.findDataFunctionsPageByProjectId(projectId, PageRequest.of(dataFunctionsPage, 5));

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("dataFunctionsList", dataFunctionsPageResult.getContent());
        model.addAttribute("dataFunctionsPage", dataFunctionsPageResult);
        model.addAttribute("requirementsCurrentPage", requirementsPage);
        model.addAttribute("transactionalFunctionsCurrentPage", transactionalFunctionsPage);

        model.addAttribute("canManageProject",
                projectAuthorizationService.canManageProject(projectId));

        model.addAttribute("canEditEstimationData",
                projectAuthorizationService.canEditEstimationData(projectId));

        model.addAttribute("canManageEffortConversions",
                projectAuthorizationService.canManageEffortConversions(projectId));

        return "fp/details :: dataFunctionsSection";
    }

    @GetMapping("/function-points/transactional-functions/update")
    public String updateTransactionalFunctionsSection(@PathVariable Long projectId,
                                                      @RequestParam(name = "requirementsPage", defaultValue = "0") int requirementsPage,
                                                      @RequestParam(name = "dataFunctionsPage", defaultValue = "0") int dataFunctionsPage,
                                                      @RequestParam(name = "transactionalFunctionsPage", defaultValue = "0") int transactionalFunctionsPage,
                                                      Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisService.findDetailedByProjectId(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToFunctionPointAdd(projectId);
        }

        Page<TransactionalFunction> transactionalFunctionsPageResult =
                functionPointAnalysisService.findTransactionalFunctionsPageByProjectId(projectId, PageRequest.of(transactionalFunctionsPage, 5));

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("transactionalFunctionsList", transactionalFunctionsPageResult.getContent());
        model.addAttribute("transactionalFunctionsPage", transactionalFunctionsPageResult);
        model.addAttribute("requirementsCurrentPage", requirementsPage);
        model.addAttribute("dataFunctionsCurrentPage", dataFunctionsPage);

        model.addAttribute("canManageProject",
                projectAuthorizationService.canManageProject(projectId));

        model.addAttribute("canEditEstimationData",
                projectAuthorizationService.canEditEstimationData(projectId));

        model.addAttribute("canManageEffortConversions",
                projectAuthorizationService.canManageEffortConversions(projectId));

        return "fp/details :: transactionalFunctionsSection";
    }

    @GetMapping("/function-points/gsc/edit")
    public String getEditGscForm(@PathVariable Long projectId, Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisService.findDetailedByProjectId(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (!projectAuthorizationService.canEditEstimationData(projectId)) {
            return redirectToFunctionPointDetails(projectId);
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToFunctionPointAdd(projectId);
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("analysis", optionalAnalysis.get());

        return "fp/gsc/edit";
    }

    @PostMapping("/function-points/gsc/edit")
    public String updateGsc(@PathVariable Long projectId,
                            @ModelAttribute("analysis") FunctionPointAnalysis formAnalysis,
                            BindingResult result,
                            Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisService.findDetailedByProjectId(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (!projectAuthorizationService.canEditEstimationData(projectId)) {
            return redirectToFunctionPointDetails(projectId);
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToFunctionPointAdd(projectId);
        }

        generalSystemCharacteristicsValidator.validate(formAnalysis, result);

        if (result.hasErrors()) {
            model.addAttribute("project", optionalProject.get());
            model.addAttribute("analysis", formAnalysis);
            return "fp/gsc/edit";
        }

        boolean updated = functionPointAnalysisService.updateGeneralSystemCharacteristics(projectId, formAnalysis);

        if (!updated) {
            return redirectToFunctionPointAdd(projectId);
        }

        return redirectToFunctionPointDetails(projectId);
    }

    @GetMapping("/function-points/delete")
    public String deleteFunctionPointAnalysis(@PathVariable Long projectId) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);

        if (optionalProject.isEmpty()) {
            return "redirect:/projects";
        }

        if (!projectAuthorizationService.canManageProject(projectId)) {
            return redirectToFunctionPointDetails(projectId);
        }

        functionPointAnalysisService.deleteByProjectId(projectId);
        return "redirect:/projects/" + projectId;
    }

    private String redirectToProjects() {
        return "redirect:/projects";
    }

    private String redirectToFunctionPointAdd(Long projectId) {
        return "redirect:/projects/" + projectId + "/function-points/add";
    }

    private String redirectToFunctionPointDetails(Long projectId) {
        return "redirect:/projects/" + projectId + "/function-points";
    }
}