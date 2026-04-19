package com.uniovi.estimacion.controllers;

import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.services.functionpoints.FunctionPointAnalysisService;
import com.uniovi.estimacion.services.projects.EstimationProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/projects")
@RequiredArgsConstructor
public class EstimationProjectController {

    private final EstimationProjectService estimationProjectService;
    private final FunctionPointAnalysisService functionPointAnalysisService;

    @GetMapping
    public String listProjects(Model model, Pageable pageable) {
        Page<EstimationProject> projectsPage = estimationProjectService.findPage(pageable);

        model.addAttribute("projectsList", projectsPage.getContent());
        model.addAttribute("projectsPage", projectsPage);

        return "project/list";
    }
    @GetMapping("/update")
    public String updateProjectsSection(@RequestParam(name = "page", defaultValue = "0") int page,
                                        Pageable pageable,
                                        Model model) {
        Page<EstimationProject> projectsPage = estimationProjectService.findPage(PageRequest.of(page, pageable.getPageSize()));

        model.addAttribute("projectsList", projectsPage.getContent());
        model.addAttribute("projectsPage", projectsPage);

        return "project/list :: projectsSection";
    }

    @GetMapping("/{projectId}")
    public String getProjectDetails(@PathVariable Long projectId, Model model) {
        Optional<EstimationProject> optionalProject = estimationProjectService.findById(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToList();
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("hasFunctionPointAnalysis",
                functionPointAnalysisService.findByProjectId(projectId).isPresent());

        return "project/details";
    }

    @GetMapping("/add")
    public String getAddForm(Model model) {
        model.addAttribute("project", new EstimationProject());
        return "project/add";
    }

    @PostMapping("/add")
    public String addProject(@ModelAttribute("project") EstimationProject project) {
        EstimationProject savedProject = estimationProjectService.create(project);
        return redirectToDetails(savedProject.getId());
    }

    @GetMapping("/edit/{projectId}")
    public String getEditForm(@PathVariable Long projectId,
                              @RequestParam(name = "returnTo", defaultValue = "list") String returnTo,
                              @RequestParam(name = "page", required = false) Integer page,
                              Model model) {
        Optional<EstimationProject> optionalProject = estimationProjectService.findById(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToList();
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("returnTo", returnTo);
        model.addAttribute("page", page);

        return "project/edit";
    }

    @PostMapping("/edit/{projectId}")
    public String editProject(@PathVariable Long projectId,
                              @RequestParam(name = "returnTo", defaultValue = "list") String returnTo,
                              @RequestParam(name = "page", required = false) Integer page,
                              @ModelAttribute("project") EstimationProject formProject) {
        boolean updated = estimationProjectService.updateBasicData(projectId, formProject);

        if (!updated) {
            return redirectToList();
        }

        if ("details".equals(returnTo)) {
            return redirectToDetails(projectId);
        }

        return redirectToList(page);
    }

    @GetMapping("/delete/{projectId}")
    public String deleteProject(@PathVariable Long projectId,
                                @RequestParam(name = "page", required = false) Integer page) {
        estimationProjectService.deleteById(projectId);
        return redirectToList(page);
    }

    @GetMapping("/{projectId}/function-points/access")
    public String accessFunctionPointAnalysis(@PathVariable Long projectId) {
        Optional<EstimationProject> optionalProject = estimationProjectService.findById(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToList();
        }

        boolean hasAnalysis = functionPointAnalysisService.findDetailedByProjectId(projectId).isPresent();

        if (hasAnalysis) {
            return "redirect:/projects/" + projectId + "/function-points";
        }

        return "redirect:/projects/" + projectId + "/function-points/add";
    }

    private String redirectToList() {
        return "redirect:/projects";
    }

    private String redirectToList(Integer page) {
        if (page == null || page < 0) {
            return "redirect:/projects";
        }

        return "redirect:/projects?page=" + page;
    }

    private String redirectToDetails(Long projectId) {
        return "redirect:/projects/" + projectId;
    }
}