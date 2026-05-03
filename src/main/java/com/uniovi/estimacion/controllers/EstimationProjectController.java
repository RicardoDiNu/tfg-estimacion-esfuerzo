package com.uniovi.estimacion.controllers;

import com.uniovi.estimacion.entities.effortconversions.DelphiEstimation;
import com.uniovi.estimacion.entities.effortconversions.transformationfunctions.TransformationFunctionConversion;
import com.uniovi.estimacion.entities.functionpoints.FunctionPointAnalysis;
import com.uniovi.estimacion.entities.projects.EstimationModule;
import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.services.effortconversions.DelphiEstimationService;
import com.uniovi.estimacion.services.effortconversions.TransformationFunctionService;
import com.uniovi.estimacion.services.functionpoints.FunctionPointAnalysisService;
import com.uniovi.estimacion.services.functionpoints.FunctionPointAnalysisSummary;
import com.uniovi.estimacion.services.functionpoints.FunctionPointCalculationService;
import com.uniovi.estimacion.services.projects.EstimationModuleService;
import com.uniovi.estimacion.services.projects.EstimationProjectService;
import com.uniovi.estimacion.validators.projects.EstimationProjectValidator;
import com.uniovi.estimacion.services.analysis.FunctionPointSizeAnalysisProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/projects")
@RequiredArgsConstructor
public class EstimationProjectController {

    private final EstimationProjectService estimationProjectService;
    private final FunctionPointAnalysisService functionPointAnalysisService;
    private final FunctionPointCalculationService functionPointCalculationService;
    private final EstimationProjectValidator estimationProjectValidator;
    private final DelphiEstimationService delphiEstimationService;
    private final EstimationModuleService estimationModuleService;
    private final FunctionPointSizeAnalysisProvider functionPointSizeAnalysisProvider;
    private final TransformationFunctionService transformationFunctionService;

    @GetMapping
    public String listProjects(Model model, Pageable pageable) {
        Page<EstimationProject> projectsPage = estimationProjectService.findPageForCurrentUser(pageable);

        model.addAttribute("projectsList", projectsPage.getContent());
        model.addAttribute("projectsPage", projectsPage);

        return "project/list";
    }

    @GetMapping("/update")
    public String updateProjectsSection(@RequestParam(name = "page", defaultValue = "0") int page,
                                        Pageable pageable,
                                        Model model) {
        Page<EstimationProject> projectsPage =
                estimationProjectService.findPageForCurrentUser(PageRequest.of(page, pageable.getPageSize()));

        model.addAttribute("projectsList", projectsPage.getContent());
        model.addAttribute("projectsPage", projectsPage);

        return "project/list :: projectsSection";
    }

    @GetMapping("/{projectId}")
    public String getProjectDetails(@PathVariable Long projectId, Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToList();
        }

        Optional<FunctionPointAnalysis> optionalFunctionPointAnalysis =
                functionPointAnalysisService.findDetailedByProjectId(projectId);

        boolean hasFunctionPointAnalysis = optionalFunctionPointAnalysis.isPresent();

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("hasFunctionPointAnalysis", hasFunctionPointAnalysis);
        model.addAttribute("hasUseCasePointAnalysis", false);

        FunctionPointAnalysisSummary functionPointResults = null;
        DelphiEstimation activeFunctionPointDelphi = null;
        Double functionPointDelphiEstimatedTotalHours = null;
        TransformationFunctionConversion activeFunctionPointTransformationConversion = null;
        Double functionPointTransformationEstimatedHours = null;

        if (hasFunctionPointAnalysis) {
            FunctionPointAnalysis functionPointAnalysis = optionalFunctionPointAnalysis.get();

            functionPointResults =
                    functionPointCalculationService.buildSummary(functionPointAnalysis);

            Optional<DelphiEstimation> optionalActiveDelphi =
                    delphiEstimationService.findDetailedActiveBySourceAnalysis(functionPointAnalysis);

            if (optionalActiveDelphi.isPresent()) {
                activeFunctionPointDelphi = optionalActiveDelphi.get();

                if (activeFunctionPointDelphi.getRegressionIntercept() != null
                        && activeFunctionPointDelphi.getRegressionSlope() != null) {

                    List<EstimationModule> modulesList =
                            estimationModuleService.findAllByProjectId(projectId);

                    Map<Long, Double> moduleSizeById =
                            functionPointSizeAnalysisProvider.buildModuleSizeById(
                                    functionPointAnalysis,
                                    modulesList
                            );

                    functionPointDelphiEstimatedTotalHours =
                            delphiEstimationService.calculateTotalEstimatedEffortHours(
                                    activeFunctionPointDelphi,
                                    moduleSizeById
                            );
                }
            }

            Optional<TransformationFunctionConversion> optionalActiveTransformationConversion =
                    transformationFunctionService.findActiveBySourceAnalysis(functionPointAnalysis);

            if (optionalActiveTransformationConversion.isPresent()) {
                activeFunctionPointTransformationConversion = optionalActiveTransformationConversion.get();

                functionPointTransformationEstimatedHours =
                        transformationFunctionService.calculateEstimatedEffortHours(
                                activeFunctionPointTransformationConversion,
                                functionPointAnalysis.getCalculatedSizeValue()
                        );
            }
        }

        model.addAttribute("functionPointResults", functionPointResults);
        model.addAttribute("activeFunctionPointDelphi", activeFunctionPointDelphi);
        model.addAttribute("functionPointDelphiEstimatedTotalHours", functionPointDelphiEstimatedTotalHours);
        model.addAttribute("activeFunctionPointTransformationConversion", activeFunctionPointTransformationConversion);
        model.addAttribute("functionPointTransformationEstimatedHours", functionPointTransformationEstimatedHours);
        model.addAttribute("functionPointTechniqueCode",
                optionalFunctionPointAnalysis
                        .map(FunctionPointAnalysis::getTechniqueCode)
                        .orElse(null));

        return "project/details";
    }

    @GetMapping("/add")
    public String getAddForm(Model model) {
        model.addAttribute("project", new EstimationProject());
        return "project/add";
    }

    @PostMapping("/add")
    public String addProject(@ModelAttribute("project") EstimationProject project,
                             BindingResult result,
                             Model model) {
        estimationProjectValidator.validate(project, result);

        if (result.hasErrors()) {
            model.addAttribute("project", project);
            return "project/add";
        }

        EstimationProject savedProject = estimationProjectService.create(project);
        return redirectToDetails(savedProject.getId());
    }

    @GetMapping("/edit/{projectId}")
    public String getEditForm(@PathVariable Long projectId,
                              @RequestParam(name = "returnTo", defaultValue = "list") String returnTo,
                              @RequestParam(name = "page", required = false) Integer page,
                              Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);

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
                              @ModelAttribute("project") EstimationProject formProject,
                              BindingResult result,
                              Model model) {
        estimationProjectValidator.validate(formProject, result);

        if (result.hasErrors()) {
            model.addAttribute("project", formProject);
            model.addAttribute("returnTo", returnTo);
            model.addAttribute("page", page);
            return "project/edit";
        }

        boolean updated = estimationProjectService.updateBasicDataForCurrentUser(projectId, formProject);

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
        estimationProjectService.deleteAccessibleByIdForCurrentUser(projectId);
        return redirectToList(page);
    }

    @GetMapping("/{projectId}/function-points/access")
    public String accessFunctionPointAnalysis(@PathVariable Long projectId) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);

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