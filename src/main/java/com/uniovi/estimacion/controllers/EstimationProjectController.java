package com.uniovi.estimacion.controllers;

import com.uniovi.estimacion.entities.EstimationProject;
import com.uniovi.estimacion.services.EstimationProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class EstimationProjectController {

    private final EstimationProjectService estimationProjectService;

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
    public String saveProject(@ModelAttribute EstimationProject project) {
        estimationProjectService.saveProject(project);
        return "redirect:/projects";
    }

    @GetMapping("/projects/{id}")
    public String getProjectDetails(@PathVariable Long id, Model model) {
        Optional<EstimationProject> optionalProject = estimationProjectService.getProject(id);

        if (optionalProject.isEmpty()) {
            return "redirect:/projects";
        }

        model.addAttribute("project", optionalProject.get());
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
}