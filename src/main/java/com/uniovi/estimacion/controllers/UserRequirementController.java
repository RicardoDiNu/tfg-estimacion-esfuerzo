package com.uniovi.estimacion.controllers;

import com.uniovi.estimacion.entities.EstimationProject;
import com.uniovi.estimacion.entities.functionpoints.DataFunction;
import com.uniovi.estimacion.entities.functionpoints.DataFunctionType;
import com.uniovi.estimacion.entities.functionpoints.TransactionalFunction;
import com.uniovi.estimacion.entities.functionpoints.TransactionalFunctionType;
import com.uniovi.estimacion.entities.requirements.UserRequirement;
import com.uniovi.estimacion.services.EstimationProjectService;
import com.uniovi.estimacion.services.functionpoints.FunctionPointAnalysisService;
import com.uniovi.estimacion.services.requirements.UserRequirementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class UserRequirementController {

    private final EstimationProjectService estimationProjectService;
    private final UserRequirementService userRequirementService;
    private final FunctionPointAnalysisService functionPointAnalysisService;

    @GetMapping("/projects/{projectId}/requirements")
    public String getRequirementList(@PathVariable Long projectId, Model model) {
        Optional<EstimationProject> optionalProject = estimationProjectService.getProject(projectId);

        if (optionalProject.isEmpty()) {
            return "redirect:/projects";
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("requirements", userRequirementService.getDetailedByProjectId(projectId));

        boolean hasFunctionPointAnalysis = functionPointAnalysisService.getDetailedByProjectId(projectId).isPresent();
        model.addAttribute("hasFunctionPointAnalysis", hasFunctionPointAnalysis);

        return "requirement/list";
    }

    @GetMapping("/projects/{projectId}/requirements/add")
    public String getAddRequirementForm(@PathVariable Long projectId, Model model) {
        Optional<EstimationProject> optionalProject = estimationProjectService.getProject(projectId);

        if (optionalProject.isEmpty()) {
            return "redirect:/projects";
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("requirement", new UserRequirement());

        return "requirement/add";
    }

    @PostMapping("/projects/{projectId}/requirements/add")
    public String addRequirement(@PathVariable Long projectId,
                                 @ModelAttribute("requirement") UserRequirement requirement) {
        Optional<EstimationProject> optionalProject = estimationProjectService.getProject(projectId);

        if (optionalProject.isEmpty()) {
            return "redirect:/projects";
        }

        requirement.setEstimationProject(optionalProject.get());
        UserRequirement savedRequirement = userRequirementService.saveRequirement(requirement);

        return "redirect:/projects/" + projectId + "/requirements/" + savedRequirement.getId();
    }

    @GetMapping("/projects/{projectId}/requirements/edit/{requirementId}")
    public String getEditRequirementForm(@PathVariable Long projectId,
                                         @PathVariable Long requirementId,
                                         Model model) {
        Optional<EstimationProject> optionalProject = estimationProjectService.getProject(projectId);
        Optional<UserRequirement> optionalRequirement =
                userRequirementService.getByIdAndProjectId(requirementId, projectId);

        if (optionalProject.isEmpty()) {
            return "redirect:/projects";
        }

        if (optionalRequirement.isEmpty()) {
            return "redirect:/projects/" + projectId + "/requirements";
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("requirement", optionalRequirement.get());

        return "requirement/edit";
    }

    @PostMapping("/projects/{projectId}/requirements/edit/{requirementId}")
    public String updateRequirement(@PathVariable Long projectId,
                                    @PathVariable Long requirementId,
                                    @ModelAttribute("requirement") UserRequirement formRequirement,
                                    Model model) {
        Optional<EstimationProject> optionalProject = estimationProjectService.getProject(projectId);
        Optional<UserRequirement> optionalRequirement =
                userRequirementService.getByIdAndProjectId(requirementId, projectId);

        if (optionalProject.isEmpty()) {
            return "redirect:/projects";
        }

        if (optionalRequirement.isEmpty()) {
            return "redirect:/projects/" + projectId + "/requirements";
        }

        if (!hasIdentifierOrName(formRequirement)) {
            model.addAttribute("project", optionalProject.get());
            model.addAttribute("validationError", true);
            formRequirement.setId(requirementId);
            model.addAttribute("requirement", formRequirement);
            return "requirement/edit";
        }

        UserRequirement existingRequirement = optionalRequirement.get();
        existingRequirement.setIdentifier(formRequirement.getIdentifier());
        existingRequirement.setName(formRequirement.getName());
        existingRequirement.setDescription(formRequirement.getDescription());

        userRequirementService.save(existingRequirement);

        return "redirect:/projects/" + projectId + "/requirements/" + requirementId;
    }

    @GetMapping("/projects/{projectId}/requirements/delete/{requirementId}")
    public String deleteRequirement(@PathVariable Long projectId,
                                    @PathVariable Long requirementId) {
        userRequirementService.deleteRequirementAndDerivedFunctions(projectId, requirementId);
        return "redirect:/projects/" + projectId + "/requirements";
    }

    @GetMapping("/projects/{projectId}/requirements/{requirementId}")
    public String getRequirementDetails(@PathVariable Long projectId,
                                        @PathVariable Long requirementId,
                                        Model model) {
        Optional<EstimationProject> optionalProject = estimationProjectService.getProject(projectId);
        Optional<UserRequirement> optionalRequirement =
                userRequirementService.getDetailedByIdAndProjectId(requirementId, projectId);

        if (optionalProject.isEmpty()) {
            return "redirect:/projects";
        }

        if (optionalRequirement.isEmpty()) {
            return "redirect:/projects/" + projectId + "/requirements";
        }

        boolean hasFunctionPointAnalysis = functionPointAnalysisService.getDetailedByProjectId(projectId).isPresent();
        if (!hasFunctionPointAnalysis) {
            return "redirect:/projects/" + projectId + "/function-points/add";
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("requirement", optionalRequirement.get());

        return "requirement/details";
    }

    @GetMapping("/projects/{projectId}/requirements/{requirementId}/data-functions/add")
    public String getAddDataFunctionFromRequirementForm(@PathVariable Long projectId,
                                                        @PathVariable Long requirementId,
                                                        Model model) {
        Optional<EstimationProject> optionalProject = estimationProjectService.getProject(projectId);
        Optional<UserRequirement> optionalRequirement =
                userRequirementService.getDetailedByIdAndProjectId(requirementId, projectId);

        if (optionalProject.isEmpty()) {
            return "redirect:/projects";
        }

        if (optionalRequirement.isEmpty()) {
            return "redirect:/projects/" + projectId + "/requirements";
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("requirement", optionalRequirement.get());
        model.addAttribute("dataFunction", new DataFunction());
        model.addAttribute("dataFunctionTypes", DataFunctionType.values());

        return "requirement/data-function-add";
    }

    @PostMapping("/projects/{projectId}/requirements/{requirementId}/data-functions/add")
    public String addDataFunctionFromRequirement(@PathVariable Long projectId,
                                                 @PathVariable Long requirementId,
                                                 @ModelAttribute DataFunction dataFunction) {
        functionPointAnalysisService.addDataFunctionToRequirement(projectId, requirementId, dataFunction);
        return "redirect:/projects/" + projectId + "/requirements/" + requirementId;
    }

    @GetMapping("/projects/{projectId}/requirements/{requirementId}/transactional-functions/add")
    public String getAddTransactionalFunctionFromRequirementForm(@PathVariable Long projectId,
                                                                 @PathVariable Long requirementId,
                                                                 Model model) {
        Optional<EstimationProject> optionalProject = estimationProjectService.getProject(projectId);
        Optional<UserRequirement> optionalRequirement =
                userRequirementService.getDetailedByIdAndProjectId(requirementId, projectId);

        if (optionalProject.isEmpty()) {
            return "redirect:/projects";
        }

        if (optionalRequirement.isEmpty()) {
            return "redirect:/projects/" + projectId + "/requirements";
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("requirement", optionalRequirement.get());
        model.addAttribute("transactionalFunction", new TransactionalFunction());
        model.addAttribute("transactionalFunctionTypes", TransactionalFunctionType.values());

        return "requirement/transactional-function-add";
    }

    @PostMapping("/projects/{projectId}/requirements/{requirementId}/transactional-functions/add")
    public String addTransactionalFunctionFromRequirement(@PathVariable Long projectId,
                                                          @PathVariable Long requirementId,
                                                          @ModelAttribute TransactionalFunction transactionalFunction) {
        functionPointAnalysisService.addTransactionalFunctionToRequirement(projectId, requirementId, transactionalFunction);
        return "redirect:/projects/" + projectId + "/requirements/" + requirementId;
    }

    private boolean hasIdentifierOrName(UserRequirement requirement) {
        return StringUtils.hasText(requirement.getIdentifier()) || StringUtils.hasText(requirement.getName());
    }

    @GetMapping("/projects/{projectId}/requirements/{requirementId}/data-functions/delete/{dataFunctionId}")
    public String deleteDataFunctionFromRequirement(@PathVariable Long projectId,
                                                    @PathVariable Long requirementId,
                                                    @PathVariable Long dataFunctionId) {
            functionPointAnalysisService.deleteDataFunctionInProject(projectId, dataFunctionId);
        return "redirect:/projects/" + projectId + "/requirements/" + requirementId;
    }

    @GetMapping("/projects/{projectId}/requirements/{requirementId}/transactional-functions/delete/{transactionalFunctionId}")
    public String deleteTransactionalFunctionFromRequirement(@PathVariable Long projectId,
                                                             @PathVariable Long requirementId,
                                                             @PathVariable Long transactionalFunctionId) {
        functionPointAnalysisService.deleteTransactionalFunctionInProject(projectId, transactionalFunctionId);
        return "redirect:/projects/" + projectId + "/requirements/" + requirementId;
    }

}