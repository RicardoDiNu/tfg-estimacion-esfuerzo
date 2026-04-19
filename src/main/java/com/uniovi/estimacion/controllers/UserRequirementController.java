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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/projects/{projectId}/requirements")
@RequiredArgsConstructor
public class UserRequirementController {

    private final EstimationProjectService estimationProjectService;
    private final UserRequirementService userRequirementService;
    private final FunctionPointAnalysisService functionPointAnalysisService;
    @GetMapping("/{requirementId}")
    public String getRequirementDetails(@PathVariable Long projectId,
                                        @PathVariable Long requirementId,
                                        @RequestParam(name = "requirementsPage", defaultValue = "0") int requirementsPage,
                                        @RequestParam(name = "dataFunctionsPage", defaultValue = "0") int dataFunctionsPage,
                                        @RequestParam(name = "transactionalFunctionsPage", defaultValue = "0") int transactionalFunctionsPage,
                                        Model model) {
        Optional<EstimationProject> optionalProject = estimationProjectService.findById(projectId);
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
        Optional<EstimationProject> optionalProject = estimationProjectService.findById(projectId);
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
        Optional<EstimationProject> optionalProject = estimationProjectService.findById(projectId);
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
        Optional<EstimationProject> optionalProject = estimationProjectService.findById(projectId);

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
                                 Model model) {
        Optional<EstimationProject> optionalProject = estimationProjectService.findById(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (!userRequirementService.hasBasicData(requirement)) {
            model.addAttribute("project", optionalProject.get());
            model.addAttribute("validationError", true);
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
        Optional<EstimationProject> optionalProject = estimationProjectService.findById(projectId);
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
                                  Model model) {
        Optional<EstimationProject> optionalProject = estimationProjectService.findById(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (!userRequirementService.hasBasicData(formRequirement)) {
            model.addAttribute("project", optionalProject.get());
            model.addAttribute("validationError", true);
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
        Optional<EstimationProject> optionalProject = estimationProjectService.findById(projectId);
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
                                  @ModelAttribute DataFunction dataFunction) {
        functionPointAnalysisService.addDataFunctionToRequirement(projectId, requirementId, dataFunction);
        return redirectToRequirementDetails(projectId, requirementId);
    }

    @GetMapping("/{requirementId}/transactional-functions/add")
    public String getAddTransactionalFunctionForm(@PathVariable Long projectId,
                                                  @PathVariable Long requirementId,
                                                  Model model) {
        Optional<EstimationProject> optionalProject = estimationProjectService.findById(projectId);
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
                                           @ModelAttribute TransactionalFunction transactionalFunction) {
        functionPointAnalysisService.addTransactionalFunctionToRequirement(projectId, requirementId, transactionalFunction);
        return redirectToRequirementDetails(projectId, requirementId);
    }

    @GetMapping("/{requirementId}/data-functions/delete/{dataFunctionId}")
    public String deleteDataFunction(@PathVariable Long projectId,
                                     @PathVariable Long requirementId,
                                     @PathVariable Long dataFunctionId) {
        functionPointAnalysisService.deleteDataFunction(projectId, dataFunctionId);
        return redirectToRequirementDetails(projectId, requirementId);
    }

    @GetMapping("/{requirementId}/transactional-functions/delete/{transactionalFunctionId}")
    public String deleteTransactionalFunction(@PathVariable Long projectId,
                                              @PathVariable Long requirementId,
                                              @PathVariable Long transactionalFunctionId) {
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