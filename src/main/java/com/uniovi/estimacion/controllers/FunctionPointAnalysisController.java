package com.uniovi.estimacion.controllers;

import com.uniovi.estimacion.entities.functionpoints.functions.DataFunction;
import com.uniovi.estimacion.entities.functionpoints.FunctionPointAnalysis;
import com.uniovi.estimacion.entities.functionpoints.functions.TransactionalFunction;
import com.uniovi.estimacion.entities.projects.EstimationModule;
import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.requirements.UserRequirement;
import com.uniovi.estimacion.services.functionpoints.FunctionPointAnalysisService;
import com.uniovi.estimacion.services.functionpoints.FunctionPointCalculationService;
import com.uniovi.estimacion.services.functionpoints.FunctionPointAnalysisSummary;
import com.uniovi.estimacion.services.projects.EstimationModuleService;
import com.uniovi.estimacion.services.projects.EstimationProjectService;
import com.uniovi.estimacion.services.requirements.UserRequirementService;
import com.uniovi.estimacion.validators.functionpoints.FunctionPointAnalysisValidator;
import com.uniovi.estimacion.validators.functionpoints.GeneralSystemCharacteristicsValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

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
    private final EstimationModuleService estimationModuleService;

    @GetMapping("/function-points/add")
    public String getCreateForm(@PathVariable Long projectId, Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
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

        List<EstimationModule> modulesList =
                estimationModuleService.findAllByProjectId(projectId);

        Map<Long, FunctionPointAnalysisSummary> moduleResultsMap = new LinkedHashMap<>();

        for (EstimationModule module : modulesList) {
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

        Page<UserRequirement> requirementsPageResult =
                userRequirementService.findPageByProjectId(projectId, PageRequest.of(requirementsPage, 5));

        Page<DataFunction> dataFunctionsPageResult =
                functionPointAnalysisService.findDataFunctionsPageByProjectId(projectId, PageRequest.of(dataFunctionsPage, 5));

        Page<TransactionalFunction> transactionalFunctionsPageResult =
                functionPointAnalysisService.findTransactionalFunctionsPageByProjectId(projectId, PageRequest.of(transactionalFunctionsPage, 5));

        model.addAttribute("project", project);
        model.addAttribute("analysis", analysis);
        model.addAttribute("results", results);
        model.addAttribute("modulesList", modulesList);
        model.addAttribute("moduleResultsMap", moduleResultsMap);

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

    @GetMapping("/function-points/edit")
    public String getEditForm(@PathVariable Long projectId, Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisService.findByProjectId(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
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

        if (optionalAnalysis.isEmpty()) {
            return redirectToFunctionPointAdd(projectId);
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("analysis", optionalAnalysis.get());

        return "fp/gsc-edit";
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

        if (optionalAnalysis.isEmpty()) {
            return redirectToFunctionPointAdd(projectId);
        }

        generalSystemCharacteristicsValidator.validate(formAnalysis, result);

        if (result.hasErrors()) {
            model.addAttribute("project", optionalProject.get());
            model.addAttribute("analysis", formAnalysis);
            return "fp/gsc-edit";
        }

        boolean updated = functionPointAnalysisService.updateGeneralSystemCharacteristics(projectId, formAnalysis);

        if (!updated) {
            return redirectToFunctionPointAdd(projectId);
        }

        return redirectToFunctionPointDetails(projectId);
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

    private String redirectToRequirementDetails(Long projectId, Long requirementId) {
        return "redirect:/projects/" + projectId + "/requirements/" + requirementId;
    }
}