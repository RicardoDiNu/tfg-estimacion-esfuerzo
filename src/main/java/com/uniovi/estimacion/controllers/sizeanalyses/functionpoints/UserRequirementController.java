package com.uniovi.estimacion.controllers.sizeanalyses.functionpoints;

import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.FunctionPointComplexity;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.DataFunction;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.DataFunctionType;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.TransactionalFunction;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.TransactionalFunctionType;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.modules.FunctionPointModule;
import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.requirements.UserRequirement;
import com.uniovi.estimacion.services.sizeanalyses.functionpoints.FunctionPointAnalysisService;
import com.uniovi.estimacion.services.sizeanalyses.functionpoints.FunctionPointModuleService;
import com.uniovi.estimacion.services.projects.EstimationProjectService;
import com.uniovi.estimacion.services.projects.ProjectAuthorizationService;
import com.uniovi.estimacion.services.sizeanalyses.functionpoints.UserRequirementService;
import com.uniovi.estimacion.validators.sizeanalyses.functionpoints.DataFunctionValidator;
import com.uniovi.estimacion.validators.sizeanalyses.functionpoints.TransactionalFunctionValidator;
import com.uniovi.estimacion.validators.sizeanalyses.functionpoints.UserRequirementValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/projects/{projectId}/function-points/modules/{moduleId}/requirements")
@RequiredArgsConstructor
public class UserRequirementController {

    private final EstimationProjectService estimationProjectService;
    private final FunctionPointModuleService functionPointModuleService;
    private final UserRequirementService userRequirementService;
    private final FunctionPointAnalysisService functionPointAnalysisService;
    private final ProjectAuthorizationService projectAuthorizationService;
    private final UserRequirementValidator userRequirementValidator;
    private final DataFunctionValidator dataFunctionValidator;
    private final TransactionalFunctionValidator transactionalFunctionValidator;

    @GetMapping("/{requirementId}")
    public String getRequirementDetails(@PathVariable Long projectId,
                                        @PathVariable Long moduleId,
                                        @PathVariable Long requirementId,
                                        @RequestParam(name = "requirementsPage", defaultValue = "0") int requirementsPage,
                                        @RequestParam(name = "dataFunctionsPage", defaultValue = "0") int dataFunctionsPage,
                                        @RequestParam(name = "transactionalFunctionsPage", defaultValue = "0") int transactionalFunctionsPage,
                                        Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<FunctionPointModule> optionalModule =
                functionPointModuleService.findByIdAndProjectId(moduleId, projectId);
        Optional<UserRequirement> optionalRequirement =
                userRequirementService.findDetailedByIdAndModuleId(requirementId, moduleId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (functionPointAnalysisService.findDetailedByProjectId(projectId).isEmpty()) {
            return redirectToFunctionPointAdd(projectId);
        }

        if (optionalModule.isEmpty()) {
            return redirectToFunctionPointDetails(projectId);
        }

        if (optionalRequirement.isEmpty()) {
            return redirectToModuleDetails(projectId, moduleId);
        }

        Page<DataFunction> dataFunctionsPageResult =
                functionPointAnalysisService.findDataFunctionsPageByRequirementId(requirementId, PageRequest.of(dataFunctionsPage, 5));

        Page<TransactionalFunction> transactionalFunctionsPageResult =
                functionPointAnalysisService.findTransactionalFunctionsPageByRequirementId(requirementId, PageRequest.of(transactionalFunctionsPage, 5));

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("module", optionalModule.get());
        model.addAttribute("requirement", optionalRequirement.get());

        model.addAttribute("requirementsPage", requirementsPage);

        model.addAttribute("dataFunctionsList", dataFunctionsPageResult.getContent());
        model.addAttribute("dataFunctionsPage", dataFunctionsPageResult);

        model.addAttribute("transactionalFunctionsList", transactionalFunctionsPageResult.getContent());
        model.addAttribute("transactionalFunctionsPage", transactionalFunctionsPageResult);

        model.addAttribute("canEditEstimationData",
                projectAuthorizationService.canEditEstimationData(projectId));

        return "fp/requirements/details";
    }

    @GetMapping("/{requirementId}/data-functions/update")
    public String updateDataFunctionsSection(@PathVariable Long projectId,
                                             @PathVariable Long moduleId,
                                             @PathVariable Long requirementId,
                                             @RequestParam(name = "requirementsPage", defaultValue = "0") int requirementsPage,
                                             @RequestParam(name = "dataFunctionsPage", defaultValue = "0") int dataFunctionsPage,
                                             @RequestParam(name = "transactionalFunctionsPage", defaultValue = "0") int transactionalFunctionsPage,
                                             Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<FunctionPointModule> optionalModule =
                functionPointModuleService.findByIdAndProjectId(moduleId, projectId);
        Optional<UserRequirement> optionalRequirement =
                userRequirementService.findDetailedByIdAndModuleId(requirementId, moduleId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalModule.isEmpty()) {
            return redirectToFunctionPointDetails(projectId);
        }

        if (optionalRequirement.isEmpty()) {
            return redirectToModuleDetails(projectId, moduleId);
        }

        Page<DataFunction> dataFunctionsPageResult =
                functionPointAnalysisService.findDataFunctionsPageByRequirementId(requirementId, PageRequest.of(dataFunctionsPage, 5));

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("module", optionalModule.get());
        model.addAttribute("requirement", optionalRequirement.get());
        model.addAttribute("dataFunctionsList", dataFunctionsPageResult.getContent());
        model.addAttribute("dataFunctionsPage", dataFunctionsPageResult);
        model.addAttribute("requirementsPage", requirementsPage);
        model.addAttribute("transactionalFunctionsCurrentPage", transactionalFunctionsPage);
        model.addAttribute("canEditEstimationData",
                projectAuthorizationService.canEditEstimationData(projectId));

        return "fp/requirements/details :: dataFunctionsSection";
    }

    @GetMapping("/{requirementId}/transactional-functions/update")
    public String updateTransactionalFunctionsSection(@PathVariable Long projectId,
                                                      @PathVariable Long moduleId,
                                                      @PathVariable Long requirementId,
                                                      @RequestParam(name = "requirementsPage", defaultValue = "0") int requirementsPage,
                                                      @RequestParam(name = "dataFunctionsPage", defaultValue = "0") int dataFunctionsPage,
                                                      @RequestParam(name = "transactionalFunctionsPage", defaultValue = "0") int transactionalFunctionsPage,
                                                      Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<FunctionPointModule> optionalModule =
                functionPointModuleService.findByIdAndProjectId(moduleId, projectId);
        Optional<UserRequirement> optionalRequirement =
                userRequirementService.findDetailedByIdAndModuleId(requirementId, moduleId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalModule.isEmpty()) {
            return redirectToFunctionPointDetails(projectId);
        }

        if (optionalRequirement.isEmpty()) {
            return redirectToModuleDetails(projectId, moduleId);
        }

        Page<TransactionalFunction> transactionalFunctionsPageResult =
                functionPointAnalysisService.findTransactionalFunctionsPageByRequirementId(requirementId, PageRequest.of(transactionalFunctionsPage, 5));

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("module", optionalModule.get());
        model.addAttribute("requirement", optionalRequirement.get());
        model.addAttribute("transactionalFunctionsList", transactionalFunctionsPageResult.getContent());
        model.addAttribute("transactionalFunctionsPage", transactionalFunctionsPageResult);
        model.addAttribute("requirementsPage", requirementsPage);
        model.addAttribute("dataFunctionsCurrentPage", dataFunctionsPage);
        model.addAttribute("canEditEstimationData",
                projectAuthorizationService.canEditEstimationData(projectId));

        return "fp/requirements/details :: transactionalFunctionsSection";
    }

    @GetMapping("/add")
    public String getAddForm(@PathVariable Long projectId,
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
            return redirectToFunctionPointDetails(projectId);
        }

        if (functionPointAnalysisService.findDetailedByProjectId(projectId).isEmpty()) {
            return redirectToFunctionPointAdd(projectId);
        }

        if (optionalModule.isEmpty()) {
            return redirectToFunctionPointDetails(projectId);
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("module", optionalModule.get());
        model.addAttribute("requirement", new UserRequirement());

        return "fp/requirements/add";
    }

    @PostMapping("/add")
    public String addRequirement(@PathVariable Long projectId,
                                 @PathVariable Long moduleId,
                                 @ModelAttribute("requirement") UserRequirement requirement,
                                 BindingResult result,
                                 Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<FunctionPointModule> optionalModule =
                functionPointModuleService.findByIdAndProjectId(moduleId, projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (!projectAuthorizationService.canEditEstimationData(projectId)) {
            return redirectToFunctionPointDetails(projectId);
        }

        if (optionalModule.isEmpty()) {
            return redirectToFunctionPointDetails(projectId);
        }

        userRequirementValidator.validate(requirement, result);

        if (result.hasErrors()) {
            model.addAttribute("project", optionalProject.get());
            model.addAttribute("module", optionalModule.get());
            model.addAttribute("requirement", requirement);
            return "fp/requirements/add";
        }

        UserRequirement savedRequirement =
                userRequirementService.createForModule(optionalModule.get(), requirement);

        return redirectToRequirementDetails(projectId, moduleId, savedRequirement.getId());
    }

    @GetMapping("/edit/{requirementId}")
    public String getEditForm(@PathVariable Long projectId,
                              @PathVariable Long moduleId,
                              @PathVariable Long requirementId,
                              Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<FunctionPointModule> optionalModule =
                functionPointModuleService.findByIdAndProjectId(moduleId, projectId);
        Optional<UserRequirement> optionalRequirement =
                userRequirementService.findByIdAndModuleId(requirementId, moduleId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (!projectAuthorizationService.canEditEstimationData(projectId)) {
            return redirectToModuleDetails(projectId, moduleId);
        }

        if (optionalModule.isEmpty()) {
            return redirectToFunctionPointDetails(projectId);
        }

        if (optionalRequirement.isEmpty()) {
            return redirectToModuleDetails(projectId, moduleId);
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("module", optionalModule.get());
        model.addAttribute("requirement", optionalRequirement.get());

        return "fp/requirements/edit";
    }

    @PostMapping("/edit/{requirementId}")
    public String editRequirement(@PathVariable Long projectId,
                                  @PathVariable Long moduleId,
                                  @PathVariable Long requirementId,
                                  @ModelAttribute("requirement") UserRequirement formRequirement,
                                  BindingResult result,
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

        userRequirementValidator.validate(formRequirement, result);

        if (result.hasErrors()) {
            model.addAttribute("project", optionalProject.get());
            model.addAttribute("module", optionalModule.get());
            formRequirement.setId(requirementId);
            model.addAttribute("requirement", formRequirement);
            return "fp/requirements/edit";
        }

        boolean updated = userRequirementService.updateBasicData(moduleId, requirementId, formRequirement);

        if (!updated) {
            return redirectToModuleDetails(projectId, moduleId);
        }

        return redirectToRequirementDetails(projectId, moduleId, requirementId);
    }

    @GetMapping("/delete/{requirementId}")
    public String deleteRequirement(@PathVariable Long projectId,
                                    @PathVariable Long moduleId,
                                    @PathVariable Long requirementId,
                                    @RequestParam(name = "requirementsPage", defaultValue = "0") int requirementsPage) {
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

        userRequirementService.deleteByIdWithDerivedFunctions(moduleId, requirementId);

        return "redirect:/projects/" + projectId
                + "/function-points/modules/" + moduleId
                + "?requirementsPage=" + requirementsPage;
    }

    @GetMapping("/{requirementId}/data-functions/add")
    public String getAddDataFunctionForm(@PathVariable Long projectId,
                                         @PathVariable Long moduleId,
                                         @PathVariable Long requirementId,
                                         Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<FunctionPointModule> optionalModule =
                functionPointModuleService.findByIdAndProjectId(moduleId, projectId);
        Optional<UserRequirement> optionalRequirement =
                userRequirementService.findDetailedByIdAndModuleId(requirementId, moduleId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (!projectAuthorizationService.canEditEstimationData(projectId)) {
            return redirectToRequirementDetails(projectId, moduleId, requirementId);
        }

        if (optionalModule.isEmpty()) {
            return redirectToFunctionPointDetails(projectId);
        }

        if (optionalRequirement.isEmpty()) {
            return redirectToModuleDetails(projectId, moduleId);
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("module", optionalModule.get());
        model.addAttribute("requirement", optionalRequirement.get());
        model.addAttribute("dataFunction", new DataFunction());
        model.addAttribute("dataFunctionTypes", DataFunctionType.values());
        model.addAttribute("complexities", FunctionPointComplexity.values());

        return "fp/functions/data-function-add";
    }

    @PostMapping("/{requirementId}/data-functions/add")
    public String addDataFunction(@PathVariable Long projectId,
                                  @PathVariable Long moduleId,
                                  @PathVariable Long requirementId,
                                  @ModelAttribute("dataFunction") DataFunction dataFunction,
                                  BindingResult result,
                                  Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<FunctionPointModule> optionalModule =
                functionPointModuleService.findByIdAndProjectId(moduleId, projectId);
        Optional<UserRequirement> optionalRequirement =
                userRequirementService.findDetailedByIdAndModuleId(requirementId, moduleId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (!projectAuthorizationService.canEditEstimationData(projectId)) {
            return redirectToRequirementDetails(projectId, moduleId, requirementId);
        }

        if (optionalModule.isEmpty()) {
            return redirectToFunctionPointDetails(projectId);
        }

        if (optionalRequirement.isEmpty()) {
            return redirectToModuleDetails(projectId, moduleId);
        }

        dataFunctionValidator.validate(dataFunction, result);

        if (result.hasErrors()) {
            model.addAttribute("project", optionalProject.get());
            model.addAttribute("module", optionalModule.get());
            model.addAttribute("requirement", optionalRequirement.get());
            model.addAttribute("dataFunction", dataFunction);
            model.addAttribute("dataFunctionTypes", DataFunctionType.values());
            model.addAttribute("complexities", FunctionPointComplexity.values());
            return "fp/functions/data-function-add";
        }

        functionPointAnalysisService.addDataFunctionToRequirement(projectId, requirementId, dataFunction);
        return redirectToRequirementDetails(projectId, moduleId, requirementId);
    }

    @GetMapping("/{requirementId}/transactional-functions/add")
    public String getAddTransactionalFunctionForm(@PathVariable Long projectId,
                                                  @PathVariable Long moduleId,
                                                  @PathVariable Long requirementId,
                                                  Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<FunctionPointModule> optionalModule =
                functionPointModuleService.findByIdAndProjectId(moduleId, projectId);
        Optional<UserRequirement> optionalRequirement =
                userRequirementService.findDetailedByIdAndModuleId(requirementId, moduleId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (!projectAuthorizationService.canEditEstimationData(projectId)) {
            return redirectToRequirementDetails(projectId, moduleId, requirementId);
        }

        if (optionalModule.isEmpty()) {
            return redirectToFunctionPointDetails(projectId);
        }

        if (optionalRequirement.isEmpty()) {
            return redirectToModuleDetails(projectId, moduleId);
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("module", optionalModule.get());
        model.addAttribute("requirement", optionalRequirement.get());
        model.addAttribute("transactionalFunction", new TransactionalFunction());
        model.addAttribute("transactionalFunctionTypes", TransactionalFunctionType.values());
        model.addAttribute("complexities", FunctionPointComplexity.values());

        return "fp/functions/transactional-function-add";
    }

    @PostMapping("/{requirementId}/transactional-functions/add")
    public String addTransactionalFunction(@PathVariable Long projectId,
                                           @PathVariable Long moduleId,
                                           @PathVariable Long requirementId,
                                           @ModelAttribute("transactionalFunction") TransactionalFunction transactionalFunction,
                                           BindingResult result,
                                           Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<FunctionPointModule> optionalModule =
                functionPointModuleService.findByIdAndProjectId(moduleId, projectId);
        Optional<UserRequirement> optionalRequirement =
                userRequirementService.findDetailedByIdAndModuleId(requirementId, moduleId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (!projectAuthorizationService.canEditEstimationData(projectId)) {
            return redirectToRequirementDetails(projectId, moduleId, requirementId);
        }

        if (optionalModule.isEmpty()) {
            return redirectToFunctionPointDetails(projectId);
        }

        if (optionalRequirement.isEmpty()) {
            return redirectToModuleDetails(projectId, moduleId);
        }

        transactionalFunctionValidator.validate(transactionalFunction, result);

        if (result.hasErrors()) {
            model.addAttribute("project", optionalProject.get());
            model.addAttribute("module", optionalModule.get());
            model.addAttribute("requirement", optionalRequirement.get());
            model.addAttribute("transactionalFunction", transactionalFunction);
            model.addAttribute("transactionalFunctionTypes", TransactionalFunctionType.values());
            model.addAttribute("complexities", FunctionPointComplexity.values());
            return "fp/functions/transactional-function-add";
        }

        functionPointAnalysisService.addTransactionalFunctionToRequirement(projectId, requirementId, transactionalFunction);
        return redirectToRequirementDetails(projectId, moduleId, requirementId);
    }

    @GetMapping("/{requirementId}/data-functions/edit/{dataFunctionId}")
    public String getEditDataFunctionFromRequirementForm(@PathVariable Long projectId,
                                                         @PathVariable Long moduleId,
                                                         @PathVariable Long requirementId,
                                                         @PathVariable Long dataFunctionId,
                                                         Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<FunctionPointModule> optionalModule =
                functionPointModuleService.findByIdAndProjectId(moduleId, projectId);
        Optional<UserRequirement> optionalRequirement =
                userRequirementService.findDetailedByIdAndModuleId(requirementId, moduleId);
        Optional<DataFunction> optionalDataFunction =
                functionPointAnalysisService.findDataFunction(projectId, dataFunctionId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (!projectAuthorizationService.canEditEstimationData(projectId)) {
            return redirectToRequirementDetails(projectId, moduleId, requirementId);
        }

        if (optionalModule.isEmpty()) {
            return redirectToFunctionPointDetails(projectId);
        }

        if (optionalRequirement.isEmpty() || optionalDataFunction.isEmpty()) {
            return redirectToModuleDetails(projectId, moduleId);
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("module", optionalModule.get());
        model.addAttribute("requirementId", requirementId);
        model.addAttribute("dataFunction", optionalDataFunction.get());
        model.addAttribute("dataFunctionTypes", DataFunctionType.values());
        model.addAttribute("complexities", FunctionPointComplexity.values());

        return "fp/functions/data-function-edit";
    }

    @PostMapping("/{requirementId}/data-functions/edit/{dataFunctionId}")
    public String updateDataFunctionFromRequirement(@PathVariable Long projectId,
                                                    @PathVariable Long moduleId,
                                                    @PathVariable Long requirementId,
                                                    @PathVariable Long dataFunctionId,
                                                    @ModelAttribute("dataFunction") DataFunction formDataFunction,
                                                    BindingResult result,
                                                    Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<FunctionPointModule> optionalModule =
                functionPointModuleService.findByIdAndProjectId(moduleId, projectId);
        Optional<UserRequirement> optionalRequirement =
                userRequirementService.findDetailedByIdAndModuleId(requirementId, moduleId);
        Optional<DataFunction> optionalDataFunction =
                functionPointAnalysisService.findDataFunction(projectId, dataFunctionId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (!projectAuthorizationService.canEditEstimationData(projectId)) {
            return redirectToRequirementDetails(projectId, moduleId, requirementId);
        }

        if (optionalModule.isEmpty()) {
            return redirectToFunctionPointDetails(projectId);
        }

        if (optionalRequirement.isEmpty() || optionalDataFunction.isEmpty()) {
            return redirectToModuleDetails(projectId, moduleId);
        }

        dataFunctionValidator.validate(formDataFunction, result);

        if (result.hasErrors()) {
            formDataFunction.setId(dataFunctionId);
            model.addAttribute("project", optionalProject.get());
            model.addAttribute("module", optionalModule.get());
            model.addAttribute("requirementId", requirementId);
            model.addAttribute("dataFunction", formDataFunction);
            model.addAttribute("dataFunctionTypes", DataFunctionType.values());
            model.addAttribute("complexities", FunctionPointComplexity.values());
            return "fp/functions/data-function-edit";
        }

        functionPointAnalysisService.updateDataFunction(projectId, dataFunctionId, formDataFunction);
        return redirectToRequirementDetails(projectId, moduleId, requirementId);
    }

    @GetMapping("/{requirementId}/transactional-functions/edit/{transactionalFunctionId}")
    public String getEditTransactionalFunctionFromRequirementForm(@PathVariable Long projectId,
                                                                  @PathVariable Long moduleId,
                                                                  @PathVariable Long requirementId,
                                                                  @PathVariable Long transactionalFunctionId,
                                                                  Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<FunctionPointModule> optionalModule =
                functionPointModuleService.findByIdAndProjectId(moduleId, projectId);
        Optional<UserRequirement> optionalRequirement =
                userRequirementService.findDetailedByIdAndModuleId(requirementId, moduleId);
        Optional<TransactionalFunction> optionalTransactionalFunction =
                functionPointAnalysisService.findTransactionalFunction(projectId, transactionalFunctionId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (!projectAuthorizationService.canEditEstimationData(projectId)) {
            return redirectToRequirementDetails(projectId, moduleId, requirementId);
        }

        if (optionalModule.isEmpty()) {
            return redirectToFunctionPointDetails(projectId);
        }

        if (optionalRequirement.isEmpty() || optionalTransactionalFunction.isEmpty()) {
            return redirectToModuleDetails(projectId, moduleId);
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("module", optionalModule.get());
        model.addAttribute("requirementId", requirementId);
        model.addAttribute("transactionalFunction", optionalTransactionalFunction.get());
        model.addAttribute("transactionalFunctionTypes", TransactionalFunctionType.values());
        model.addAttribute("complexities", FunctionPointComplexity.values());

        return "fp/functions/transactional-function-edit";
    }

    @PostMapping("/{requirementId}/transactional-functions/edit/{transactionalFunctionId}")
    public String updateTransactionalFunctionFromRequirement(@PathVariable Long projectId,
                                                             @PathVariable Long moduleId,
                                                             @PathVariable Long requirementId,
                                                             @PathVariable Long transactionalFunctionId,
                                                             @ModelAttribute("transactionalFunction") TransactionalFunction formTransactionalFunction,
                                                             BindingResult result,
                                                             Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<FunctionPointModule> optionalModule =
                functionPointModuleService.findByIdAndProjectId(moduleId, projectId);
        Optional<UserRequirement> optionalRequirement =
                userRequirementService.findDetailedByIdAndModuleId(requirementId, moduleId);
        Optional<TransactionalFunction> optionalTransactionalFunction =
                functionPointAnalysisService.findTransactionalFunction(projectId, transactionalFunctionId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (!projectAuthorizationService.canEditEstimationData(projectId)) {
            return redirectToRequirementDetails(projectId, moduleId, requirementId);
        }

        if (optionalModule.isEmpty()) {
            return redirectToFunctionPointDetails(projectId);
        }

        if (optionalRequirement.isEmpty() || optionalTransactionalFunction.isEmpty()) {
            return redirectToModuleDetails(projectId, moduleId);
        }

        transactionalFunctionValidator.validate(formTransactionalFunction, result);

        if (result.hasErrors()) {
            formTransactionalFunction.setId(transactionalFunctionId);
            model.addAttribute("project", optionalProject.get());
            model.addAttribute("module", optionalModule.get());
            model.addAttribute("requirementId", requirementId);
            model.addAttribute("transactionalFunction", formTransactionalFunction);
            model.addAttribute("transactionalFunctionTypes", TransactionalFunctionType.values());
            model.addAttribute("complexities", FunctionPointComplexity.values());
            return "fp/functions/transactional-function-edit";
        }

        functionPointAnalysisService.updateTransactionalFunction(projectId, transactionalFunctionId, formTransactionalFunction);
        return redirectToRequirementDetails(projectId, moduleId, requirementId);
    }

    @GetMapping("/{requirementId}/data-functions/delete/{dataFunctionId}")
    public String deleteDataFunction(@PathVariable Long projectId,
                                     @PathVariable Long moduleId,
                                     @PathVariable Long requirementId,
                                     @PathVariable Long dataFunctionId) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<FunctionPointModule> optionalModule =
                functionPointModuleService.findByIdAndProjectId(moduleId, projectId);
        Optional<UserRequirement> optionalRequirement =
                userRequirementService.findByIdAndModuleId(requirementId, moduleId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (!projectAuthorizationService.canEditEstimationData(projectId)) {
            return redirectToRequirementDetails(projectId, moduleId, requirementId);
        }

        if (optionalModule.isEmpty()) {
            return redirectToFunctionPointDetails(projectId);
        }

        if (optionalRequirement.isEmpty()) {
            return redirectToModuleDetails(projectId, moduleId);
        }

        functionPointAnalysisService.deleteDataFunction(projectId, dataFunctionId);
        return redirectToRequirementDetails(projectId, moduleId, requirementId);
    }

    @GetMapping("/{requirementId}/transactional-functions/delete/{transactionalFunctionId}")
    public String deleteTransactionalFunction(@PathVariable Long projectId,
                                              @PathVariable Long moduleId,
                                              @PathVariable Long requirementId,
                                              @PathVariable Long transactionalFunctionId) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<FunctionPointModule> optionalModule =
                functionPointModuleService.findByIdAndProjectId(moduleId, projectId);
        Optional<UserRequirement> optionalRequirement =
                userRequirementService.findByIdAndModuleId(requirementId, moduleId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (!projectAuthorizationService.canEditEstimationData(projectId)) {
            return redirectToRequirementDetails(projectId, moduleId, requirementId);
        }

        if (optionalModule.isEmpty()) {
            return redirectToFunctionPointDetails(projectId);
        }

        if (optionalRequirement.isEmpty()) {
            return redirectToModuleDetails(projectId, moduleId);
        }

        functionPointAnalysisService.deleteTransactionalFunction(projectId, transactionalFunctionId);
        return redirectToRequirementDetails(projectId, moduleId, requirementId);
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

    private String redirectToModuleDetails(Long projectId, Long moduleId) {
        return "redirect:/projects/" + projectId + "/function-points/modules/" + moduleId;
    }

    private String redirectToRequirementDetails(Long projectId, Long moduleId, Long requirementId) {
        return "redirect:/projects/" + projectId
                + "/function-points/modules/" + moduleId
                + "/requirements/" + requirementId;
    }
}