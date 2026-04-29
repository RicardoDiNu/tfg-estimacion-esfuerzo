package com.uniovi.estimacion.controllers;

import com.uniovi.estimacion.entities.analysis.SizeAnalysis;
import com.uniovi.estimacion.entities.effortconversions.DelphiEstimation;
import com.uniovi.estimacion.entities.effortconversions.DelphiExpertEstimate;
import com.uniovi.estimacion.entities.effortconversions.DelphiIteration;
import com.uniovi.estimacion.entities.projects.EstimationModule;
import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.services.analysis.SizeAnalysisProvider;
import com.uniovi.estimacion.services.analysis.SizeAnalysisProviderRegistry;
import com.uniovi.estimacion.services.effortconversions.DelphiEstimationService;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/projects/{projectId}/size-analyses/{sourceTechniqueCode}/delphi")
@RequiredArgsConstructor
public class DelphiEstimationController {

    private final EstimationProjectService estimationProjectService;
    private final EstimationModuleService estimationModuleService;
    private final DelphiEstimationService delphiEstimationService;
    private final DelphiEstimationValidator delphiEstimationValidator;
    private final DelphiIterationValidator delphiIterationValidator;
    private final SizeAnalysisProviderRegistry sizeAnalysisProviderRegistry;

    @GetMapping("/access")
    public String accessDelphi(@PathVariable Long projectId,
                               @PathVariable String sourceTechniqueCode) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<? extends SizeAnalysis> optionalAnalysis =
                findSourceAnalysis(projectId, sourceTechniqueCode);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToSourceAnalysisAdd(projectId, sourceTechniqueCode);
        }

        Optional<DelphiEstimation> optionalActiveEstimation =
                delphiEstimationService.findActiveBySourceAnalysis(optionalAnalysis.get());

        if (optionalActiveEstimation.isPresent()) {
            return redirectToDelphiDetails(
                    projectId,
                    sourceTechniqueCode,
                    optionalActiveEstimation.get().getId()
            );
        }

        return redirectToDelphiAdd(projectId, sourceTechniqueCode);
    }

    @GetMapping("/add")
    public String getCreateForm(@PathVariable Long projectId,
                                @PathVariable String sourceTechniqueCode,
                                Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<? extends SizeAnalysis> optionalAnalysis =
                findSourceAnalysis(projectId, sourceTechniqueCode);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToSourceAnalysisAdd(projectId, sourceTechniqueCode);
        }

        loadCreateFormModel(
                optionalProject.get(),
                optionalAnalysis.get(),
                sourceTechniqueCode,
                new DelphiEstimationCreateForm(),
                model
        );

        return "effortconversions/delphi/add";
    }

    @PostMapping("/add")
    public String createInitialEstimation(@PathVariable Long projectId,
                                          @PathVariable String sourceTechniqueCode,
                                          @ModelAttribute("createForm") DelphiEstimationCreateForm createForm,
                                          BindingResult result,
                                          Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<? extends SizeAnalysis> optionalAnalysis =
                findSourceAnalysis(projectId, sourceTechniqueCode);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToSourceAnalysisAdd(projectId, sourceTechniqueCode);
        }

        EstimationProject project = optionalProject.get();
        SizeAnalysis analysis = optionalAnalysis.get();

        List<EstimationModule> modulesList = estimationModuleService.findAllByProjectId(projectId);
        Map<Long, Double> moduleSizeById =
                getSourceAnalysisProvider(sourceTechniqueCode).buildModuleSizeById(
                        analysis,
                        modulesList
                );

        delphiEstimationValidator.validate(createForm, result);

        if (!delphiEstimationService.canStartCalibration(moduleSizeById)) {
            result.reject("delphi.validation.notEnoughModules");
        }

        if (result.hasErrors()) {
            loadCreateFormModel(project, analysis, sourceTechniqueCode, createForm, model);
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

        return redirectToIterationAdd(projectId, sourceTechniqueCode, estimation.getId());
    }

    @GetMapping("/{delphiEstimationId}")
    public String getDetails(@PathVariable Long projectId,
                             @PathVariable String sourceTechniqueCode,
                             @PathVariable Long delphiEstimationId,
                             Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<? extends SizeAnalysis> optionalAnalysis =
                findSourceAnalysis(projectId, sourceTechniqueCode);
        Optional<DelphiEstimation> optionalEstimation =
                delphiEstimationService.findDetailedByIdAndProjectId(delphiEstimationId, projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToSourceAnalysisAdd(projectId, sourceTechniqueCode);
        }

        if (optionalEstimation.isEmpty()
                || !belongsToSourceTechnique(optionalEstimation.get(), sourceTechniqueCode)) {
            return redirectToSourceAnalysisDetails(projectId, sourceTechniqueCode);
        }

        EstimationProject project = optionalProject.get();
        SizeAnalysis analysis = optionalAnalysis.get();
        DelphiEstimation estimation = optionalEstimation.get();

        List<EstimationModule> modulesList = estimationModuleService.findAllByProjectId(projectId);
        Map<Long, Double> moduleSizeById =
                getSourceAnalysisProvider(sourceTechniqueCode).buildModuleSizeById(
                        analysis,
                        modulesList
                );

        Map<Long, Double> moduleEstimatedEffortById = new LinkedHashMap<>();
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
        model.addAttribute("sourceTechniqueCode", sourceTechniqueCode);
        model.addAttribute("sourceAnalysisDetailsPath",
                getSourceAnalysisProvider(sourceTechniqueCode).getDetailsPath(projectId));
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
                                      @PathVariable String sourceTechniqueCode,
                                      @PathVariable Long delphiEstimationId,
                                      Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<DelphiEstimation> optionalEstimation =
                delphiEstimationService.findByIdAndProjectId(delphiEstimationId, projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalEstimation.isEmpty()
                || !belongsToSourceTechnique(optionalEstimation.get(), sourceTechniqueCode)) {
            return redirectToSourceAnalysisDetails(projectId, sourceTechniqueCode);
        }

        DelphiEstimation estimation = optionalEstimation.get();

        if (delphiEstimationService.isFinished(estimation)) {
            return redirectToDelphiDetails(projectId, sourceTechniqueCode, delphiEstimationId);
        }

        DelphiIterationForm iterationForm = new DelphiIterationForm();
        initializeIterationForm(iterationForm, estimation.getExpertCount());

        loadIterationFormModel(
                optionalProject.get(),
                estimation,
                sourceTechniqueCode,
                iterationForm,
                model
        );

        return "effortconversions/delphi/iteration-add";
    }

    @PostMapping("/{delphiEstimationId}/iterations/add")
    public String addIteration(@PathVariable Long projectId,
                               @PathVariable String sourceTechniqueCode,
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

        if (optionalEstimation.isEmpty()
                || !belongsToSourceTechnique(optionalEstimation.get(), sourceTechniqueCode)) {
            return redirectToSourceAnalysisDetails(projectId, sourceTechniqueCode);
        }

        DelphiEstimation estimation = optionalEstimation.get();

        if (delphiEstimationService.isFinished(estimation)) {
            return redirectToDelphiDetails(projectId, sourceTechniqueCode, delphiEstimationId);
        }

        iterationForm.setExpectedExpertCount(estimation.getExpertCount());
        delphiIterationValidator.validate(iterationForm, result);

        if (result.hasErrors()) {
            initializeIterationForm(iterationForm, estimation.getExpertCount());
            loadIterationFormModel(
                    optionalProject.get(),
                    estimation,
                    sourceTechniqueCode,
                    iterationForm,
                    model
            );
            return "effortconversions/delphi/iteration-add";
        }

        List<DelphiExpertEstimate> expertEstimates = iterationForm.getExpertEstimates().stream()
                .map(this::toEntity)
                .toList();

        delphiEstimationService.registerIteration(delphiEstimationId, expertEstimates);

        return redirectToDelphiDetails(projectId, sourceTechniqueCode, delphiEstimationId);
    }

    @GetMapping("/{delphiEstimationId}/delete")
    public String deleteEstimation(@PathVariable Long projectId,
                                   @PathVariable String sourceTechniqueCode,
                                   @PathVariable Long delphiEstimationId) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<DelphiEstimation> optionalEstimation =
                delphiEstimationService.findByIdAndProjectId(delphiEstimationId, projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalEstimation.isPresent()
                && belongsToSourceTechnique(optionalEstimation.get(), sourceTechniqueCode)) {
            delphiEstimationService.deleteByIdAndProjectId(delphiEstimationId, projectId);
        }

        return redirectToSourceAnalysisDetails(projectId, sourceTechniqueCode);
    }

    @GetMapping("/{delphiEstimationId}/iterations/{iterationNumber}")
    public String getIterationDetails(@PathVariable Long projectId,
                                      @PathVariable String sourceTechniqueCode,
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

        if (optionalEstimation.isEmpty()
                || !belongsToSourceTechnique(optionalEstimation.get(), sourceTechniqueCode)) {
            return redirectToSourceAnalysisDetails(projectId, sourceTechniqueCode);
        }

        DelphiEstimation estimation = optionalEstimation.get();

        Optional<DelphiIteration> optionalIteration =
                estimation.getIterations().stream()
                        .filter(iteration -> iteration.getIterationNumber().equals(iterationNumber))
                        .findFirst();

        if (optionalIteration.isEmpty()) {
            return redirectToDelphiDetails(projectId, sourceTechniqueCode, delphiEstimationId);
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("estimation", estimation);
        model.addAttribute("iteration", optionalIteration.get());
        model.addAttribute("sourceTechniqueCode", sourceTechniqueCode);
        model.addAttribute("sourceAnalysisDetailsPath",
                getSourceAnalysisProvider(sourceTechniqueCode).getDetailsPath(projectId));

        return "effortconversions/delphi/iteration-details";
    }

    private void loadCreateFormModel(EstimationProject project,
                                     SizeAnalysis analysis,
                                     String sourceTechniqueCode,
                                     DelphiEstimationCreateForm createForm,
                                     Model model) {
        List<EstimationModule> modulesList = estimationModuleService.findAllByProjectId(project.getId());
        Map<Long, Double> moduleSizeById =
                getSourceAnalysisProvider(sourceTechniqueCode).buildModuleSizeById(
                        analysis,
                        modulesList
                );

        model.addAttribute("project", project);
        model.addAttribute("analysis", analysis);
        model.addAttribute("sourceTechniqueCode", sourceTechniqueCode);
        model.addAttribute("sourceAnalysisDetailsPath",
                getSourceAnalysisProvider(sourceTechniqueCode).getDetailsPath(project.getId()));
        model.addAttribute("modulesList", modulesList);
        model.addAttribute("moduleSizeById", moduleSizeById);
        model.addAttribute("canStartDelphi", delphiEstimationService.canStartCalibration(moduleSizeById));
        model.addAttribute("createForm", createForm);
    }

    private void loadIterationFormModel(EstimationProject project,
                                        DelphiEstimation estimation,
                                        String sourceTechniqueCode,
                                        DelphiIterationForm iterationForm,
                                        Model model) {
        model.addAttribute("project", project);
        model.addAttribute("estimation", estimation);
        model.addAttribute("sourceTechniqueCode", sourceTechniqueCode);
        model.addAttribute("sourceAnalysisDetailsPath",
                getSourceAnalysisProvider(sourceTechniqueCode).getDetailsPath(project.getId()));
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

    private boolean belongsToSourceTechnique(DelphiEstimation estimation, String sourceTechniqueCode) {
        return estimation.getSourceTechniqueCode() != null
                && estimation.getSourceTechniqueCode().equals(sourceTechniqueCode);
    }

    private SizeAnalysisProvider getSourceAnalysisProvider(String sourceTechniqueCode) {
        return sizeAnalysisProviderRegistry.getByTechniqueCode(sourceTechniqueCode);
    }

    private Optional<? extends SizeAnalysis> findSourceAnalysis(Long projectId,
                                                                String sourceTechniqueCode) {
        return getSourceAnalysisProvider(sourceTechniqueCode).findDetailedByProjectId(projectId);
    }

    private String redirectToProjects() {
        return "redirect:/projects";
    }

    private String redirectToSourceAnalysisAdd(Long projectId, String sourceTechniqueCode) {
        SizeAnalysisProvider provider = getSourceAnalysisProvider(sourceTechniqueCode);
        return "redirect:" + provider.getAddPath(projectId);
    }

    private String redirectToSourceAnalysisDetails(Long projectId, String sourceTechniqueCode) {
        SizeAnalysisProvider provider = getSourceAnalysisProvider(sourceTechniqueCode);
        return "redirect:" + provider.getDetailsPath(projectId);
    }

    private String redirectToDelphiAdd(Long projectId, String sourceTechniqueCode) {
        return "redirect:/projects/" + projectId
                + "/size-analyses/" + sourceTechniqueCode
                + "/delphi/add";
    }

    private String redirectToDelphiDetails(Long projectId,
                                           String sourceTechniqueCode,
                                           Long delphiEstimationId) {
        return "redirect:/projects/" + projectId
                + "/size-analyses/" + sourceTechniqueCode
                + "/delphi/" + delphiEstimationId;
    }

    private String redirectToIterationAdd(Long projectId,
                                          String sourceTechniqueCode,
                                          Long delphiEstimationId) {
        return "redirect:/projects/" + projectId
                + "/size-analyses/" + sourceTechniqueCode
                + "/delphi/" + delphiEstimationId
                + "/iterations/add";
    }
}