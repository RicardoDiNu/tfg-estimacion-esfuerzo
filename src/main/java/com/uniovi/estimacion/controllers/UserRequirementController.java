package com.uniovi.estimacion.controllers;

import com.uniovi.estimacion.entities.functionpoints.DataFunction;
import com.uniovi.estimacion.entities.functionpoints.DataFunctionType;
import com.uniovi.estimacion.entities.functionpoints.TransactionalFunction;
import com.uniovi.estimacion.entities.functionpoints.TransactionalFunctionType;
import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.requirements.UserRequirement;
import com.uniovi.estimacion.services.functionpoints.FunctionPointAnalysisService;
import com.uniovi.estimacion.services.projects.EstimationProjectService;
import com.uniovi.estimacion.services.requirements.UserRequirementService;
import com.uniovi.estimacion.validators.functionpoints.DataFunctionValidator;
import com.uniovi.estimacion.validators.functionpoints.TransactionalFunctionValidator;
import com.uniovi.estimacion.validators.requirements.UserRequirementValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/projects/{projectId}/requirements")
@RequiredArgsConstructor
public class UserRequirementController {

    private final EstimationProjectService estimationProjectService;
    private final UserRequirementService userRequirementService;
    private final FunctionPointAnalysisService functionPointAnalysisService;
    private final UserRequirementValidator userRequirementValidator;
    private final DataFunctionValidator dataFunctionValidator;
    private final TransactionalFunctionValidator transactionalFunctionValidator;

    @GetMapping("/{requirementId}")
    public String getRequirementDetails(@PathVariable Long projectId,
                                        @PathVariable Long requirementId,
                                        @RequestParam(name = "requirementsPage", defaultValue = "0") int requirementsPage,
                                        @RequestParam(name = "dataFunctionsPage", defaultValue = "0") int dataFunctionsPage,
                                        @RequestParam(name = "transactionalFunctionsPage", defaultValue = "0") int transactionalFunctionsPage,
                                        Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<UserRequirement> optionalRequirement =
                userRequirementService.findByIdAndProjectId(requirementId, projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (functionPointAnalysisService.findDetailedByProjectId(projectId).isEmpty()) {
            return redirectToFunctionPointAdd(projectId);
        }

        if (optionalRequirement.isEmpty()) {
            return redirectToFunctionPointDetails(projectId);
        }

        Page<DataFunction> dataFunctionsPageResult =
                functionPointAnalysisService.findDataFunctionsPageByRequirementId(requirementId, PageRequest.of(dataFunctionsPage, 5));

        Page<TransactionalFunction> transactionalFunctionsPageResult =
                functionPointAnalysisService.findTransactionalFunctionsPageByRequirementId(requirementId, PageRequest.of(transactionalFunctionsPage, 5));

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("requirement", optionalRequirement.get());

        model.addAttribute("requirementsPage", requirementsPage);

        model.addAttribute("dataFunctionsList", dataFunctionsPageResult.getContent());
        model.addAttribute("dataFunctionsPage", dataFunctionsPageResult);

        model.addAttribute("transactionalFunctionsList", transactionalFunctionsPageResult.getContent());
        model.addAttribute("transactionalFunctionsPage", transactionalFunctionsPageResult);

        return "requirement/details";
    }

    @GetMapping("/{requirementId}/data-functions/update")
    public String updateDataFunctionsSection(@PathVariable Long projectId,
                                             @PathVariable Long requirementId,
                                             @RequestParam(name = "requirementsPage", defaultValue = "0") int requirementsPage,
                                             @RequestParam(name = "dataFunctionsPage", defaultValue = "0") int dataFunctionsPage,
                                             @RequestParam(name = "transactionalFunctionsPage", defaultValue = "0") int transactionalFunctionsPage,
                                             Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<UserRequirement> optionalRequirement =
                userRequirementService.findByIdAndProjectId(requirementId, projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalRequirement.isEmpty()) {
            return redirectToFunctionPointDetails(projectId);
        }

        Page<DataFunction> dataFunctionsPageResult =
                functionPointAnalysisService.findDataFunctionsPageByRequirementId(requirementId, PageRequest.of(dataFunctionsPage, 5));

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("requirement", optionalRequirement.get());
        model.addAttribute("dataFunctionsList", dataFunctionsPageResult.getContent());
        model.addAttribute("dataFunctionsPage", dataFunctionsPageResult);
        model.addAttribute("requirementsPage", requirementsPage);
        model.addAttribute("transactionalFunctionsCurrentPage", transactionalFunctionsPage);

        return "requirement/details :: dataFunctionsSection";
    }

    @GetMapping("/{requirementId}/transactional-functions/update")
    public String updateTransactionalFunctionsSection(@PathVariable Long projectId,
                                                      @PathVariable Long requirementId,
                                                      @RequestParam(name = "requirementsPage", defaultValue = "0") int requirementsPage,
                                                      @RequestParam(name = "dataFunctionsPage", defaultValue = "0") int dataFunctionsPage,
                                                      @RequestParam(name = "transactionalFunctionsPage", defaultValue = "0") int transactionalFunctionsPage,
                                                      Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<UserRequirement> optionalRequirement =
                userRequirementService.findByIdAndProjectId(requirementId, projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalRequirement.isEmpty()) {
            return redirectToFunctionPointDetails(projectId);
        }

        Page<TransactionalFunction> transactionalFunctionsPageResult =
                functionPointAnalysisService.findTransactionalFunctionsPageByRequirementId(requirementId, PageRequest.of(transactionalFunctionsPage, 5));

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("requirement", optionalRequirement.get());
        model.addAttribute("transactionalFunctionsList", transactionalFunctionsPageResult.getContent());
        model.addAttribute("transactionalFunctionsPage", transactionalFunctionsPageResult);
        model.addAttribute("requirementsPage", requirementsPage);
        model.addAttribute("dataFunctionsCurrentPage", dataFunctionsPage);

        return "requirement/details :: transactionalFunctionsSection";
    }

    @GetMapping("/add")
    public String getAddForm(@PathVariable Long projectId, Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (functionPointAnalysisService.findDetailedByProjectId(projectId).isEmpty()) {
            return redirectToFunctionPointAdd(projectId);
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("requirement", new UserRequirement());

        return "requirement/add";
    }

    @PostMapping("/add")
    public String addRequirement(@PathVariable Long projectId,
                                 @ModelAttribute("requirement") UserRequirement requirement,
                                 BindingResult result,
                                 Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        userRequirementValidator.validate(requirement, result);

        if (result.hasErrors()) {
            model.addAttribute("project", optionalProject.get());
            model.addAttribute("requirement", requirement);
            return "requirement/add";
        }

        UserRequirement savedRequirement =
                userRequirementService.createForProject(optionalProject.get(), requirement);

        return redirectToRequirementDetails(projectId, savedRequirement.getId());
    }

    @GetMapping("/edit/{requirementId}")
    public String getEditForm(@PathVariable Long projectId,
                              @PathVariable Long requirementId,
                              Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<UserRequirement> optionalRequirement =
                userRequirementService.findByIdAndProjectId(requirementId, projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalRequirement.isEmpty()) {
            return redirectToFunctionPointDetails(projectId);
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("requirement", optionalRequirement.get());

        return "requirement/edit";
    }

    @PostMapping("/edit/{requirementId}")
    public String editRequirement(@PathVariable Long projectId,
                                  @PathVariable Long requirementId,
                                  @ModelAttribute("requirement") UserRequirement formRequirement,
                                  BindingResult result,
                                  Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        userRequirementValidator.validate(formRequirement, result);

        if (result.hasErrors()) {
            model.addAttribute("project", optionalProject.get());
            formRequirement.setId(requirementId);
            model.addAttribute("requirement", formRequirement);
            return "requirement/edit";
        }

        boolean updated = userRequirementService.updateBasicData(projectId, requirementId, formRequirement);

        if (!updated) {
            return redirectToFunctionPointDetails(projectId);
        }

        return redirectToRequirementDetails(projectId, requirementId);
    }

    @GetMapping("/delete/{requirementId}")
    public String deleteRequirement(@PathVariable Long projectId,
                                    @PathVariable Long requirementId,
                                    @RequestParam(name = "requirementsPage", defaultValue = "0") int requirementsPage,
                                    @RequestParam(name = "dataFunctionsPage", defaultValue = "0") int dataFunctionsPage,
                                    @RequestParam(name = "transactionalFunctionsPage", defaultValue = "0") int transactionalFunctionsPage) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        userRequirementService.deleteByIdWithDerivedFunctions(projectId, requirementId);

        return "redirect:/projects/" + projectId
                + "/function-points?requirementsPage=" + requirementsPage
                + "&dataFunctionsPage=" + dataFunctionsPage
                + "&transactionalFunctionsPage=" + transactionalFunctionsPage;
    }

    @GetMapping("/{requirementId}/data-functions/add")
    public String getAddDataFunctionForm(@PathVariable Long projectId,
                                         @PathVariable Long requirementId,
                                         Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<UserRequirement> optionalRequirement =
                userRequirementService.findDetailedByIdAndProjectId(requirementId, projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalRequirement.isEmpty()) {
            return redirectToFunctionPointDetails(projectId);
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("requirement", optionalRequirement.get());
        model.addAttribute("dataFunction", new DataFunction());
        model.addAttribute("dataFunctionTypes", DataFunctionType.values());

        return "fp/data-function-add";
    }

    @PostMapping("/{requirementId}/data-functions/add")
    public String addDataFunction(@PathVariable Long projectId,
                                  @PathVariable Long requirementId,
                                  @ModelAttribute("dataFunction") DataFunction dataFunction,
                                  BindingResult result,
                                  Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<UserRequirement> optionalRequirement =
                userRequirementService.findDetailedByIdAndProjectId(requirementId, projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalRequirement.isEmpty()) {
            return redirectToFunctionPointDetails(projectId);
        }

        dataFunctionValidator.validate(dataFunction, result);

        if (result.hasErrors()) {
            model.addAttribute("project", optionalProject.get());
            model.addAttribute("requirement", optionalRequirement.get());
            model.addAttribute("dataFunction", dataFunction);
            model.addAttribute("dataFunctionTypes", DataFunctionType.values());
            return "fp/data-function-add";
        }

        functionPointAnalysisService.addDataFunctionToRequirement(projectId, requirementId, dataFunction);
        return redirectToRequirementDetails(projectId, requirementId);
    }

    @GetMapping("/{requirementId}/transactional-functions/add")
    public String getAddTransactionalFunctionForm(@PathVariable Long projectId,
                                                  @PathVariable Long requirementId,
                                                  Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<UserRequirement> optionalRequirement =
                userRequirementService.findDetailedByIdAndProjectId(requirementId, projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalRequirement.isEmpty()) {
            return redirectToFunctionPointDetails(projectId);
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("requirement", optionalRequirement.get());
        model.addAttribute("transactionalFunction", new TransactionalFunction());
        model.addAttribute("transactionalFunctionTypes", TransactionalFunctionType.values());

        return "fp/transactional-function-add";
    }

    @PostMapping("/{requirementId}/transactional-functions/add")
    public String addTransactionalFunction(@PathVariable Long projectId,
                                           @PathVariable Long requirementId,
                                           @ModelAttribute("transactionalFunction") TransactionalFunction transactionalFunction,
                                           BindingResult result,
                                           Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<UserRequirement> optionalRequirement =
                userRequirementService.findDetailedByIdAndProjectId(requirementId, projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalRequirement.isEmpty()) {
            return redirectToFunctionPointDetails(projectId);
        }

        transactionalFunctionValidator.validate(transactionalFunction, result);

        if (result.hasErrors()) {
            model.addAttribute("project", optionalProject.get());
            model.addAttribute("requirement", optionalRequirement.get());
            model.addAttribute("transactionalFunction", transactionalFunction);
            model.addAttribute("transactionalFunctionTypes", TransactionalFunctionType.values());
            return "fp/transactional-function-add";
        }

        functionPointAnalysisService.addTransactionalFunctionToRequirement(projectId, requirementId, transactionalFunction);
        return redirectToRequirementDetails(projectId, requirementId);
    }

    @GetMapping("/{requirementId}/data-functions/edit/{dataFunctionId}")
    public String getEditDataFunctionFromRequirementForm(@PathVariable Long projectId,
                                                         @PathVariable Long requirementId,
                                                         @PathVariable Long dataFunctionId,
                                                         Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<DataFunction> optionalDataFunction =
                functionPointAnalysisService.findDataFunction(projectId, dataFunctionId);

        if (optionalProject.isEmpty() || optionalDataFunction.isEmpty()) {
            return redirectToProjects();
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("requirementId", requirementId);
        model.addAttribute("dataFunction", optionalDataFunction.get());
        model.addAttribute("dataFunctionTypes", DataFunctionType.values());

        return "fp/data-function-edit";
    }

    @PostMapping("/{requirementId}/data-functions/edit/{dataFunctionId}")
    public String updateDataFunctionFromRequirement(@PathVariable Long projectId,
                                                    @PathVariable Long requirementId,
                                                    @PathVariable Long dataFunctionId,
                                                    @ModelAttribute("dataFunction") DataFunction formDataFunction,
                                                    BindingResult result,
                                                    Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<DataFunction> optionalDataFunction =
                functionPointAnalysisService.findDataFunction(projectId, dataFunctionId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalDataFunction.isEmpty()) {
            return redirectToFunctionPointDetails(projectId);
        }

        dataFunctionValidator.validate(formDataFunction, result);

        if (result.hasErrors()) {
            formDataFunction.setId(dataFunctionId);
            model.addAttribute("project", optionalProject.get());
            model.addAttribute("requirementId", requirementId);
            model.addAttribute("dataFunction", formDataFunction);
            model.addAttribute("dataFunctionTypes", DataFunctionType.values());
            return "fp/data-function-edit";
        }

        functionPointAnalysisService.updateDataFunction(projectId, dataFunctionId, formDataFunction);
        return redirectToRequirementDetails(projectId, requirementId);
    }

    @GetMapping("/{requirementId}/transactional-functions/edit/{transactionalFunctionId}")
    public String getEditTransactionalFunctionFromRequirementForm(@PathVariable Long projectId,
                                                                  @PathVariable Long requirementId,
                                                                  @PathVariable Long transactionalFunctionId,
                                                                  Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<TransactionalFunction> optionalTransactionalFunction =
                functionPointAnalysisService.findTransactionalFunction(projectId, transactionalFunctionId);

        if (optionalProject.isEmpty() || optionalTransactionalFunction.isEmpty()) {
            return redirectToProjects();
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("requirementId", requirementId);
        model.addAttribute("transactionalFunction", optionalTransactionalFunction.get());
        model.addAttribute("transactionalFunctionTypes", TransactionalFunctionType.values());

        return "fp/transactional-function-edit";
    }

    @PostMapping("/{requirementId}/transactional-functions/edit/{transactionalFunctionId}")
    public String updateTransactionalFunctionFromRequirement(@PathVariable Long projectId,
                                                             @PathVariable Long requirementId,
                                                             @PathVariable Long transactionalFunctionId,
                                                             @ModelAttribute("transactionalFunction") TransactionalFunction formTransactionalFunction,
                                                             BindingResult result,
                                                             Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<TransactionalFunction> optionalTransactionalFunction =
                functionPointAnalysisService.findTransactionalFunction(projectId, transactionalFunctionId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalTransactionalFunction.isEmpty()) {
            return redirectToFunctionPointDetails(projectId);
        }

        transactionalFunctionValidator.validate(formTransactionalFunction, result);

        if (result.hasErrors()) {
            formTransactionalFunction.setId(transactionalFunctionId);
            model.addAttribute("project", optionalProject.get());
            model.addAttribute("requirementId", requirementId);
            model.addAttribute("transactionalFunction", formTransactionalFunction);
            model.addAttribute("transactionalFunctionTypes", TransactionalFunctionType.values());
            return "fp/transactional-function-edit";
        }

        functionPointAnalysisService.updateTransactionalFunction(projectId, transactionalFunctionId, formTransactionalFunction);
        return redirectToRequirementDetails(projectId, requirementId);
    }

    @GetMapping("/{requirementId}/data-functions/delete/{dataFunctionId}")
    public String deleteDataFunction(@PathVariable Long projectId,
                                     @PathVariable Long requirementId,
                                     @PathVariable Long dataFunctionId) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        functionPointAnalysisService.deleteDataFunction(projectId, dataFunctionId);
        return redirectToRequirementDetails(projectId, requirementId);
    }

    @GetMapping("/{requirementId}/transactional-functions/delete/{transactionalFunctionId}")
    public String deleteTransactionalFunction(@PathVariable Long projectId,
                                              @PathVariable Long requirementId,
                                              @PathVariable Long transactionalFunctionId) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        functionPointAnalysisService.deleteTransactionalFunction(projectId, transactionalFunctionId);
        return redirectToRequirementDetails(projectId, requirementId);
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