package com.uniovi.estimacion.controllers.sizeanalyses.functionpoints;

import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.services.projects.EstimationProjectService;
import com.uniovi.estimacion.services.projects.ProjectAuthorizationService;
import com.uniovi.estimacion.services.sizeanalyses.functionpoints.FunctionPointAnalysisService;
import com.uniovi.estimacion.validators.sizeanalyses.functionpoints.FunctionPointWeightMatrixValidator;
import com.uniovi.estimacion.web.forms.sizeanalyses.functionpoints.FunctionPointWeightMatrixForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/projects/{projectId}/function-points/weights")
@RequiredArgsConstructor
public class FunctionPointWeightMatrixController {

    private final EstimationProjectService estimationProjectService;
    private final FunctionPointAnalysisService functionPointAnalysisService;
    private final FunctionPointWeightMatrixValidator functionPointWeightMatrixValidator;
    private final ProjectAuthorizationService projectAuthorizationService;

    @GetMapping("/edit")
    public String getEditForm(@PathVariable Long projectId, Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (!projectAuthorizationService.canManageProject(projectId)) {
            return redirectToFunctionPointDetails(projectId);
        }

        Optional<FunctionPointWeightMatrixForm> optionalForm =
                functionPointAnalysisService.buildWeightMatrixForm(projectId);

        if (optionalForm.isEmpty()) {
            return redirectToFunctionPointAdd(projectId);
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("weightMatrixForm", optionalForm.get());

        return "fp/weights/edit";
    }

    @PostMapping("/edit")
    public String updateWeightMatrix(@PathVariable Long projectId,
                                     @ModelAttribute("weightMatrixForm") FunctionPointWeightMatrixForm weightMatrixForm,
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

        functionPointWeightMatrixValidator.validate(weightMatrixForm, result);

        if (result.hasErrors()) {
            model.addAttribute("project", optionalProject.get());
            model.addAttribute("weightMatrixForm", weightMatrixForm);
            return "fp/weights/edit";
        }

        boolean updated =
                functionPointAnalysisService.updateWeightMatrix(projectId, weightMatrixForm);

        if (!updated) {
            return redirectToFunctionPointAdd(projectId);
        }

        return redirectToFunctionPointDetails(projectId);
    }

    @PostMapping("/reset")
    public String resetWeightMatrix(@PathVariable Long projectId) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (!projectAuthorizationService.canManageProject(projectId)) {
            return redirectToFunctionPointDetails(projectId);
        }

        functionPointAnalysisService.resetWeightMatrixToDefault(projectId);

        return redirectToFunctionPointDetails(projectId);
    }

    private String redirectToProjects() {
        return "redirect:/projects";
    }

    private String redirectToFunctionPointDetails(Long projectId) {
        return "redirect:/projects/" + projectId + "/function-points";
    }

    private String redirectToFunctionPointAdd(Long projectId) {
        return "redirect:/projects/" + projectId + "/function-points/add";
    }
}