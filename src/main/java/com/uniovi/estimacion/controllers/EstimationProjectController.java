package com.uniovi.estimacion.controllers;

import com.uniovi.estimacion.entities.EstimationProject;
import com.uniovi.estimacion.services.EstimationProjectService;
import com.uniovi.estimacion.services.functionpoints.FunctionPointAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class EstimationProjectController {

    private final EstimationProjectService estimationProjectService;
    private final FunctionPointAnalysisService functionPointAnalysisService;

    @GetMapping("/projects")
    public String getProjectList(Model model) {
        model.addAttribute("projectsList", estimationProjectService.getProjects());
        return "project/list";
    }

    @GetMapping("/projects/add")
    public String getAddProjectForm(Model model) {
        model.addAttribute("project", new EstimationProject());
        return "project/add";
    }

    @PostMapping("/projects/add")
    public String addProject(@ModelAttribute("project") EstimationProject project) {
        estimationProjectService.saveProject(project);
        return "redirect:/projects/" + project.getId();
    }

    @GetMapping("/projects/{id}")
    public String getProjectDetails(@PathVariable Long id, Model model) {
        Optional<EstimationProject> optionalProject = estimationProjectService.getProject(id);

        if (optionalProject.isEmpty()) {
            return "redirect:/projects";
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("hasFunctionPointAnalysis", functionPointAnalysisService.getByProjectId(id).isPresent());

        return "project/details";
    }

    @GetMapping("/projects/edit/{id}")
    public String getEditProjectForm(@PathVariable Long id,
                                     @RequestParam(name = "returnTo", defaultValue = "list") String returnTo,
                                     Model model) {
        Optional<EstimationProject> optionalProject = estimationProjectService.getProject(id);

        if (optionalProject.isEmpty()) {
            return "redirect:/projects";
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("returnTo", returnTo);
        return "project/edit";
    }

    @PostMapping("/projects/edit/{id}")
    public String updateProject(@PathVariable Long id,
                                @RequestParam(name = "returnTo", defaultValue = "list") String returnTo,
                                @ModelAttribute EstimationProject project) {
        Optional<EstimationProject> optionalProject = estimationProjectService.getProject(id);

        if (optionalProject.isEmpty()) {
            return "redirect:/projects";
        }

        EstimationProject originalProject = optionalProject.get();
        originalProject.setName(project.getName());
        originalProject.setDescription(project.getDescription());

        estimationProjectService.saveProject(originalProject);

        if ("details".equals(returnTo)) {
            return "redirect:/projects/" + id;
        }

        return "redirect:/projects";
    }

    @GetMapping("/projects/delete/{id}")
    public String deleteProject(@PathVariable Long id) {
        estimationProjectService.deleteProject(id);
        return "redirect:/projects";
    }
    @GetMapping("/projects/{projectId}/function-points/access")
    public String accessFunctionPointAnalysis(@PathVariable Long projectId) {
        Optional<EstimationProject> optionalProject = estimationProjectService.getProject(projectId);

        if (optionalProject.isEmpty()) {
            return "redirect:/projects";
        }

        boolean hasAnalysis = functionPointAnalysisService.getDetailedByProjectId(projectId).isPresent();

        if (hasAnalysis) {
            return "redirect:/projects/" + projectId + "/function-points";
        }

        return "redirect:/projects/" + projectId + "/function-points/add";
    }

}