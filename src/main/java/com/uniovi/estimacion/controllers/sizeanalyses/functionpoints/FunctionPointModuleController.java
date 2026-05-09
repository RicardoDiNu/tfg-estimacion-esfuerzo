package com.uniovi.estimacion.controllers.sizeanalyses.functionpoints;

import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.FunctionPointAnalysis;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.DataFunction;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.TransactionalFunction;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.modules.FunctionPointModule;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.requirements.UserRequirement;
import com.uniovi.estimacion.services.projects.EstimationProjectService;
import com.uniovi.estimacion.services.projects.ProjectAuthorizationService;
import com.uniovi.estimacion.services.sizeanalyses.functionpoints.FunctionPointAnalysisService;
import com.uniovi.estimacion.services.sizeanalyses.functionpoints.FunctionPointAnalysisSummary;
import com.uniovi.estimacion.services.sizeanalyses.functionpoints.FunctionPointCalculationService;
import com.uniovi.estimacion.services.sizeanalyses.functionpoints.FunctionPointModuleService;
import com.uniovi.estimacion.services.sizeanalyses.functionpoints.UserRequirementService;
import com.uniovi.estimacion.validators.sizeanalyses.functionpoints.FunctionPointModuleValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/projects/{projectId}/function-points/modules")
@RequiredArgsConstructor
public class FunctionPointModuleController {

    private final EstimationProjectService estimationProjectService;
    private final ProjectAuthorizationService projectAuthorizationService;

    private final FunctionPointAnalysisService functionPointAnalysisService;
    private final FunctionPointModuleService functionPointModuleService;
    private final FunctionPointModuleValidator functionPointModuleValidator;
    private final UserRequirementService userRequirementService;
    private final FunctionPointCalculationService functionPointCalculationService;

    @GetMapping("/add")
    public String getAddForm(@PathVariable Long projectId, Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (!projectAuthorizationService.canEditEstimationData(projectId)) {
            return redirectToFunctionPointDetails(projectId);
        }

        if (functionPointAnalysisService.findByProjectId(projectId).isEmpty()) {
            return redirectToFunctionPointAccess(projectId);
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("module", new FunctionPointModule());
        addAuthorizationAttributes(model, projectId);

        return "fp/modules/add";
    }

    @PostMapping("/add")
    public String addModule(@PathVariable Long projectId,
                            @ModelAttribute("module") FunctionPointModule module,
                            BindingResult result,
                            Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (!projectAuthorizationService.canEditEstimationData(projectId)) {
            return redirectToFunctionPointDetails(projectId);
        }

        if (functionPointAnalysisService.findByProjectId(projectId).isEmpty()) {
            return redirectToFunctionPointAccess(projectId);
        }

        functionPointModuleValidator.validate(module, result);

        if (result.hasErrors()) {
            model.addAttribute("project", optionalProject.get());
            model.addAttribute("module", module);
            addAuthorizationAttributes(model, projectId);
            return "fp/modules/add";
        }

        FunctionPointModule savedModule =
                functionPointModuleService.createForProject(optionalProject.get(), module);

        return redirectToModuleDetails(projectId, savedModule.getId());
    }

    @GetMapping("/{moduleId}")
    public String getModuleDetails(@PathVariable Long projectId,
                                   @PathVariable Long moduleId,
                                   @RequestParam(name = "requirementsPage", defaultValue = "0") int requirementsPage,
                                   @RequestParam(name = "dataFunctionsPage", defaultValue = "0") int dataFunctionsPage,
                                   @RequestParam(name = "transactionalFunctionsPage", defaultValue = "0") int transactionalFunctionsPage,
                                   Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisService.findDetailedByProjectId(projectId);
        Optional<FunctionPointModule> optionalModule =
                functionPointModuleService.findByIdAndProjectId(moduleId, projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToFunctionPointAccess(projectId);
        }

        if (optionalModule.isEmpty()) {
            return redirectToFunctionPointDetails(projectId);
        }

        FunctionPointAnalysis analysis = optionalAnalysis.get();

        Page<UserRequirement> requirementsPageResult =
                userRequirementService.findPageByModuleId(moduleId, PageRequest.of(requirementsPage, 5));

        Page<DataFunction> dataFunctionsPageResult =
                functionPointAnalysisService.findDataFunctionsPageByModuleId(moduleId, PageRequest.of(dataFunctionsPage, 5));

        Page<TransactionalFunction> transactionalFunctionsPageResult =
                functionPointAnalysisService.findTransactionalFunctionsPageByModuleId(moduleId, PageRequest.of(transactionalFunctionsPage, 5));

        List<DataFunction> allDataFunctions =
                functionPointAnalysisService.findAllDataFunctionsByModuleId(moduleId);

        List<TransactionalFunction> allTransactionalFunctions =
                functionPointAnalysisService.findAllTransactionalFunctionsByModuleId(moduleId);

        FunctionPointAnalysisSummary moduleResults =
                functionPointCalculationService.buildModuleSummary(analysis, allDataFunctions, allTransactionalFunctions);

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("analysis", analysis);
        model.addAttribute("module", optionalModule.get());
        model.addAttribute("moduleResults", moduleResults);

        model.addAttribute("requirementsList", requirementsPageResult.getContent());
        model.addAttribute("requirementsPage", requirementsPageResult);

        model.addAttribute("dataFunctionsList", dataFunctionsPageResult.getContent());
        model.addAttribute("dataFunctionsPage", dataFunctionsPageResult);

        model.addAttribute("transactionalFunctionsList", transactionalFunctionsPageResult.getContent());
        model.addAttribute("transactionalFunctionsPage", transactionalFunctionsPageResult);

        model.addAttribute("dataFunctionsCurrentPage", dataFunctionsPageResult.getNumber());
        model.addAttribute("transactionalFunctionsCurrentPage", transactionalFunctionsPageResult.getNumber());

        addAuthorizationAttributes(model, projectId);

        return "fp/modules/details";
    }

    @GetMapping("/{moduleId}/edit")
    public String getEditForm(@PathVariable Long projectId,
                              @PathVariable Long moduleId,
                              Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<FunctionPointModule> optionalModule =
                functionPointModuleService.findByIdAndProjectId(moduleId, projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (!projectAuthorizationService.canEditEstimationData(projectId)) {
            return redirectToModuleDetails(projectId, moduleId);
        }

        if (optionalModule.isEmpty()) {
            return redirectToFunctionPointDetails(projectId);
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("module", optionalModule.get());
        addAuthorizationAttributes(model, projectId);

        return "fp/modules/edit";
    }

    @PostMapping("/{moduleId}/edit")
    public String editModule(@PathVariable Long projectId,
                             @PathVariable Long moduleId,
                             @ModelAttribute("module") FunctionPointModule formModule,
                             BindingResult result,
                             Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (!projectAuthorizationService.canEditEstimationData(projectId)) {
            return redirectToModuleDetails(projectId, moduleId);
        }

        functionPointModuleValidator.validate(formModule, result);

        if (result.hasErrors()) {
            model.addAttribute("project", optionalProject.get());
            formModule.setId(moduleId);
            model.addAttribute("module", formModule);
            addAuthorizationAttributes(model, projectId);
            return "fp/modules/edit";
        }

        boolean updated = functionPointModuleService.updateBasicData(projectId, moduleId, formModule);

        if (!updated) {
            return redirectToFunctionPointDetails(projectId);
        }

        return redirectToModuleDetails(projectId, moduleId);
    }

    @GetMapping("/delete/{moduleId}")
    public String deleteModule(@PathVariable Long projectId,
                               @PathVariable Long moduleId) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (!projectAuthorizationService.canEditEstimationData(projectId)) {
            return redirectToModuleDetails(projectId, moduleId);
        }

        functionPointModuleService.deleteByIdWithContents(projectId, moduleId);

        return redirectToFunctionPointDetails(projectId);
    }

    @GetMapping("/{moduleId}/requirements/update")
    public String updateRequirementsSection(@PathVariable Long projectId,
                                            @PathVariable Long moduleId,
                                            @RequestParam(name = "requirementsPage", defaultValue = "0") int requirementsPage,
                                            @RequestParam(name = "dataFunctionsPage", defaultValue = "0") int dataFunctionsPage,
                                            @RequestParam(name = "transactionalFunctionsPage", defaultValue = "0") int transactionalFunctionsPage,
                                            Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<FunctionPointModule> optionalModule =
                functionPointModuleService.findByIdAndProjectId(moduleId, projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalModule.isEmpty()) {
            return redirectToFunctionPointDetails(projectId);
        }

        Page<UserRequirement> requirementsPageResult =
                userRequirementService.findPageByModuleId(moduleId, PageRequest.of(requirementsPage, 5));

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("module", optionalModule.get());
        model.addAttribute("requirementsList", requirementsPageResult.getContent());
        model.addAttribute("requirementsPage", requirementsPageResult);
        model.addAttribute("dataFunctionsCurrentPage", dataFunctionsPage);
        model.addAttribute("transactionalFunctionsCurrentPage", transactionalFunctionsPage);

        addAuthorizationAttributes(model, projectId);

        return "fp/modules/details :: requirementsSection";
    }

    @GetMapping("/{moduleId}/data-functions/update")
    public String updateDataFunctionsSection(@PathVariable Long projectId,
                                             @PathVariable Long moduleId,
                                             @RequestParam(name = "requirementsPage", defaultValue = "0") int requirementsPage,
                                             @RequestParam(name = "dataFunctionsPage", defaultValue = "0") int dataFunctionsPage,
                                             @RequestParam(name = "transactionalFunctionsPage", defaultValue = "0") int transactionalFunctionsPage,
                                             Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<FunctionPointModule> optionalModule =
                functionPointModuleService.findByIdAndProjectId(moduleId, projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalModule.isEmpty()) {
            return redirectToFunctionPointDetails(projectId);
        }

        Page<DataFunction> dataFunctionsPageResult =
                functionPointAnalysisService.findDataFunctionsPageByModuleId(moduleId, PageRequest.of(dataFunctionsPage, 5));

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("module", optionalModule.get());
        model.addAttribute("dataFunctionsList", dataFunctionsPageResult.getContent());
        model.addAttribute("dataFunctionsPage", dataFunctionsPageResult);
        model.addAttribute("requirementsCurrentPage", requirementsPage);
        model.addAttribute("transactionalFunctionsCurrentPage", transactionalFunctionsPage);

        addAuthorizationAttributes(model, projectId);

        return "fp/modules/details :: dataFunctionsSection";
    }

    @GetMapping("/{moduleId}/transactional-functions/update")
    public String updateTransactionalFunctionsSection(@PathVariable Long projectId,
                                                      @PathVariable Long moduleId,
                                                      @RequestParam(name = "requirementsPage", defaultValue = "0") int requirementsPage,
                                                      @RequestParam(name = "dataFunctionsPage", defaultValue = "0") int dataFunctionsPage,
                                                      @RequestParam(name = "transactionalFunctionsPage", defaultValue = "0") int transactionalFunctionsPage,
                                                      Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<FunctionPointModule> optionalModule =
                functionPointModuleService.findByIdAndProjectId(moduleId, projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalModule.isEmpty()) {
            return redirectToFunctionPointDetails(projectId);
        }

        Page<TransactionalFunction> transactionalFunctionsPageResult =
                functionPointAnalysisService.findTransactionalFunctionsPageByModuleId(moduleId, PageRequest.of(transactionalFunctionsPage, 5));

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("module", optionalModule.get());
        model.addAttribute("transactionalFunctionsList", transactionalFunctionsPageResult.getContent());
        model.addAttribute("transactionalFunctionsPage", transactionalFunctionsPageResult);
        model.addAttribute("requirementsCurrentPage", requirementsPage);
        model.addAttribute("dataFunctionsCurrentPage", dataFunctionsPage);

        addAuthorizationAttributes(model, projectId);

        return "fp/modules/details :: transactionalFunctionsSection";
    }

    private void addAuthorizationAttributes(Model model, Long projectId) {
        model.addAttribute("canManageProject",
                projectAuthorizationService.canManageProject(projectId));

        model.addAttribute("canEditEstimationData",
                projectAuthorizationService.canEditEstimationData(projectId));

        model.addAttribute("canManageEffortConversions",
                projectAuthorizationService.canManageEffortConversions(projectId));
    }

    private String redirectToProjects() {
        return "redirect:/projects";
    }

    private String redirectToFunctionPointAccess(Long projectId) {
        return "redirect:/projects/" + projectId + "/function-points/access";
    }

    private String redirectToFunctionPointDetails(Long projectId) {
        return "redirect:/projects/" + projectId + "/function-points";
    }

    private String redirectToModuleDetails(Long projectId, Long moduleId) {
        return "redirect:/projects/" + projectId + "/function-points/modules/" + moduleId;
    }
}