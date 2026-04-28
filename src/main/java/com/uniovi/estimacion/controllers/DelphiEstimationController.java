package com.uniovi.estimacion.controllers;

import com.uniovi.estimacion.entities.effortconversions.DelphiEstimation;
import com.uniovi.estimacion.entities.effortconversions.DelphiExpertEstimate;
import com.uniovi.estimacion.entities.functionpoints.FunctionPointAnalysis;
import com.uniovi.estimacion.entities.projects.EstimationModule;
import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.services.effortconversions.DelphiEstimationService;
import com.uniovi.estimacion.services.functionpoints.FunctionPointAnalysisService;
import com.uniovi.estimacion.services.functionpoints.FunctionPointCalculationService;
import com.uniovi.estimacion.services.projects.EstimationModuleService;
import com.uniovi.estimacion.services.projects.EstimationProjectService;
import com.uniovi.estimacion.validators.effortconversions.DelphiEstimationValidator;
import com.uniovi.estimacion.validators.effortconversions.DelphiIterationValidator;
import com.uniovi.estimacion.web.forms.effortconversions.DelphiEstimationCreateForm;
import com.uniovi.estimacion.web.forms.effortconversions.DelphiExpertEstimateForm;
import com.uniovi.estimacion.web.forms.effortconversions.DelphiIterationForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/projects/{projectId}/function-points/delphi")
@RequiredArgsConstructor
public class DelphiEstimationController {

    private final EstimationProjectService estimationProjectService;
    private final FunctionPointAnalysisService functionPointAnalysisService;
    private final FunctionPointCalculationService functionPointCalculationService;
    private final EstimationModuleService estimationModuleService;
    private final DelphiEstimationService delphiEstimationService;
    private final DelphiEstimationValidator delphiEstimationValidator;
    private final DelphiIterationValidator delphiIterationValidator;

    @GetMapping("/access")
    public String accessDelphi(@PathVariable Long projectId) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisService.findDetailedByProjectId(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToFunctionPointAdd(projectId);
        }

        Optional<DelphiEstimation> optionalActiveEstimation =
                delphiEstimationService.findActiveBySourceAnalysis(optionalAnalysis.get());

        if (optionalActiveEstimation.isPresent()) {
            return redirectToDelphiDetails(projectId, optionalActiveEstimation.get().getId());
        }

        return redirectToDelphiAdd(projectId);
    }

    @GetMapping("/add")
    public String getCreateForm(@PathVariable Long projectId, Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisService.findDetailedByProjectId(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToFunctionPointAdd(projectId);
        }

        loadCreateFormModel(
                optionalProject.get(),
                optionalAnalysis.get(),
                new DelphiEstimationCreateForm(),
                model
        );

        return "effortconversions/delphi/add";
    }

    @PostMapping("/add")
    public String createInitialEstimation(@PathVariable Long projectId,
                                          @ModelAttribute("createForm") DelphiEstimationCreateForm createForm,
                                          BindingResult result,
                                          Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisService.findDetailedByProjectId(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToFunctionPointAdd(projectId);
        }

        EstimationProject project = optionalProject.get();
        FunctionPointAnalysis analysis = optionalAnalysis.get();

        List<EstimationModule> modulesList = estimationModuleService.findAllByProjectId(projectId);
        Map<Long, Double> moduleSizeById =
                delphiEstimationService.buildModuleSizeById(
                        analysis,
                        modulesList,
                        functionPointAnalysisService,
                        functionPointCalculationService
                );

        delphiEstimationValidator.validate(createForm, result);

        if (!delphiEstimationService.canStartCalibration(moduleSizeById)) {
            result.reject("delphi.validation.notEnoughModules");
        }

        if (result.hasErrors()) {
            loadCreateFormModel(project, analysis, createForm, model);
            return "effortconversions/delphi/add";
        }

        DelphiEstimation estimation = delphiEstimationService.createInitialEstimation(
                analysis,
                modulesList,
                moduleSizeById,
                createForm.getAcceptableDeviationPercentage(),
                createForm.getMaximumIterations(),
                createForm.getExpertCount()
        );

        return redirectToIterationAdd(projectId, estimation.getId());
    }

    @GetMapping("/{delphiEstimationId}")
    public String getDetails(@PathVariable Long projectId,
                             @PathVariable Long delphiEstimationId,
                             Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisService.findDetailedByProjectId(projectId);
        Optional<DelphiEstimation> optionalEstimation =
                delphiEstimationService.findDetailedByIdAndProjectId(delphiEstimationId, projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToFunctionPointAdd(projectId);
        }

        if (optionalEstimation.isEmpty()) {
            return redirectToFunctionPointDetails(projectId);
        }

        EstimationProject project = optionalProject.get();
        FunctionPointAnalysis analysis = optionalAnalysis.get();
        DelphiEstimation estimation = optionalEstimation.get();

        List<EstimationModule> modulesList = estimationModuleService.findAllByProjectId(projectId);
        Map<Long, Double> moduleSizeById =
                delphiEstimationService.buildModuleSizeById(
                        analysis,
                        modulesList,
                        functionPointAnalysisService,
                        functionPointCalculationService
                );

        Map<Long, Double> moduleEstimatedEffortById = new java.util.LinkedHashMap<>();
        boolean hasFinalCalibration = delphiEstimationService.isFinished(estimation);
        boolean canAddIteration = !hasFinalCalibration;
        Double totalEstimatedHours = null;

        if (hasFinalCalibration) {
            for (EstimationModule module : modulesList) {
                Double moduleSize = moduleSizeById.get(module.getId());

                if (moduleSize != null && moduleSize > 0) {
                    moduleEstimatedEffortById.put(
                            module.getId(),
                            delphiEstimationService.calculateEstimatedEffortHours(estimation, moduleSize)
                    );
                }
            }

            totalEstimatedHours =
                    delphiEstimationService.calculateTotalEstimatedEffortHours(estimation, moduleSizeById);
        }

        model.addAttribute("project", project);
        model.addAttribute("analysis", analysis);
        model.addAttribute("estimation", estimation);
        model.addAttribute("modulesList", modulesList);
        model.addAttribute("moduleSizeById", moduleSizeById);
        model.addAttribute("moduleEstimatedEffortById", moduleEstimatedEffortById);
        model.addAttribute("totalEstimatedHours", totalEstimatedHours);
        model.addAttribute("hasFinalCalibration", hasFinalCalibration);
        model.addAttribute("canAddIteration", canAddIteration);

        return "effortconversions/delphi/details";
    }

    @GetMapping("/{delphiEstimationId}/iterations/add")
    public String getAddIterationForm(@PathVariable Long projectId,
                                      @PathVariable Long delphiEstimationId,
                                      Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<DelphiEstimation> optionalEstimation =
                delphiEstimationService.findByIdAndProjectId(delphiEstimationId, projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalEstimation.isEmpty()) {
            return redirectToFunctionPointDetails(projectId);
        }

        DelphiEstimation estimation = optionalEstimation.get();

        if (delphiEstimationService.isFinished(estimation)) {
            return redirectToDelphiDetails(projectId, delphiEstimationId);
        }

        DelphiIterationForm iterationForm = new DelphiIterationForm();
        initializeIterationForm(iterationForm, estimation.getExpertCount());

        loadIterationFormModel(optionalProject.get(), estimation, iterationForm, model);
        return "effortconversions/delphi/iteration-add";
    }

    @PostMapping("/{delphiEstimationId}/iterations/add")
    public String addIteration(@PathVariable Long projectId,
                               @PathVariable Long delphiEstimationId,
                               @ModelAttribute("iterationForm") DelphiIterationForm iterationForm,
                               BindingResult result,
                               Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<DelphiEstimation> optionalEstimation =
                delphiEstimationService.findByIdAndProjectId(delphiEstimationId, projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalEstimation.isEmpty()) {
            return redirectToFunctionPointDetails(projectId);
        }

        DelphiEstimation estimation = optionalEstimation.get();

        if (delphiEstimationService.isFinished(estimation)) {
            return redirectToDelphiDetails(projectId, delphiEstimationId);
        }

        iterationForm.setExpectedExpertCount(estimation.getExpertCount());
        delphiIterationValidator.validate(iterationForm, result);

        if (result.hasErrors()) {
            initializeIterationForm(iterationForm, estimation.getExpertCount());
            loadIterationFormModel(optionalProject.get(), estimation, iterationForm, model);
            return "effortconversions/delphi/iteration-add";
        }

        List<DelphiExpertEstimate> expertEstimates = iterationForm.getExpertEstimates().stream()
                .map(this::toEntity)
                .toList();

        delphiEstimationService.registerIteration(delphiEstimationId, expertEstimates);

        return redirectToDelphiDetails(projectId, delphiEstimationId);
    }

    @GetMapping("/{delphiEstimationId}/delete")
    public String deleteEstimation(@PathVariable Long projectId,
                                   @PathVariable Long delphiEstimationId) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        delphiEstimationService.deleteByIdAndProjectId(delphiEstimationId, projectId);

        return redirectToFunctionPointDetails(projectId);
    }

    @GetMapping("/{delphiEstimationId}/iterations/{iterationNumber}")
    public String getIterationDetails(@PathVariable Long projectId,
                                      @PathVariable Long delphiEstimationId,
                                      @PathVariable Integer iterationNumber,
                                      Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<DelphiEstimation> optionalEstimation =
                delphiEstimationService.findDetailedByIdAndProjectId(delphiEstimationId, projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalEstimation.isEmpty()) {
            return redirectToFunctionPointDetails(projectId);
        }

        DelphiEstimation estimation = optionalEstimation.get();

        Optional<com.uniovi.estimacion.entities.effortconversions.DelphiIteration> optionalIteration =
                estimation.getIterations().stream()
                        .filter(iteration -> iteration.getIterationNumber().equals(iterationNumber))
                        .findFirst();

        if (optionalIteration.isEmpty()) {
            return redirectToDelphiDetails(projectId, delphiEstimationId);
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("estimation", estimation);
        model.addAttribute("iteration", optionalIteration.get());

        return "effortconversions/delphi/iteration-details";
    }


    private void loadCreateFormModel(EstimationProject project,
                                     FunctionPointAnalysis analysis,
                                     DelphiEstimationCreateForm createForm,
                                     Model model) {
        List<EstimationModule> modulesList = estimationModuleService.findAllByProjectId(project.getId());
        Map<Long, Double> moduleSizeById =
                delphiEstimationService.buildModuleSizeById(
                        analysis,
                        modulesList,
                        functionPointAnalysisService,
                        functionPointCalculationService
                );

        model.addAttribute("project", project);
        model.addAttribute("analysis", analysis);
        model.addAttribute("modulesList", modulesList);
        model.addAttribute("moduleSizeById", moduleSizeById);
        model.addAttribute("canStartDelphi", delphiEstimationService.canStartCalibration(moduleSizeById));
        model.addAttribute("createForm", createForm);
    }

    private void loadIterationFormModel(EstimationProject project,
                                        DelphiEstimation estimation,
                                        DelphiIterationForm iterationForm,
                                        Model model) {
        model.addAttribute("project", project);
        model.addAttribute("estimation", estimation);
        model.addAttribute("iterationForm", iterationForm);
    }

    private DelphiExpertEstimate toEntity(DelphiExpertEstimateForm form) {
        DelphiExpertEstimate entity = new DelphiExpertEstimate();
        entity.setEvaluatorAlias(form.getEvaluatorAlias() != null ? form.getEvaluatorAlias().trim() : null);
        entity.setMinimumModuleEstimatedEffortHours(form.getMinimumModuleEstimatedEffortHours());
        entity.setMaximumModuleEstimatedEffortHours(form.getMaximumModuleEstimatedEffortHours());
        entity.setComments(form.getComments() != null ? form.getComments().trim() : null);
        return entity;
    }

    private void initializeIterationForm(DelphiIterationForm iterationForm, int expectedExpertCount) {
        iterationForm.setExpectedExpertCount(expectedExpertCount);

        while (iterationForm.getExpertEstimates().size() < expectedExpertCount) {
            iterationForm.getExpertEstimates().add(new DelphiExpertEstimateForm());
        }
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

    private String redirectToDelphiAdd(Long projectId) {
        return "redirect:/projects/" + projectId + "/function-points/delphi/add";
    }

    private String redirectToDelphiDetails(Long projectId, Long delphiEstimationId) {
        return "redirect:/projects/" + projectId + "/function-points/delphi/" + delphiEstimationId;
    }

    private String redirectToIterationAdd(Long projectId, Long delphiEstimationId) {
        return "redirect:/projects/" + projectId + "/function-points/delphi/" + delphiEstimationId + "/iterations/add";
    }
}