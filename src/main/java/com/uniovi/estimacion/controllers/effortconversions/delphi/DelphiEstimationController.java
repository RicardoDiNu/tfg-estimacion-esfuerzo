package com.uniovi.estimacion.controllers.effortconversions.delphi;

import com.uniovi.estimacion.entities.sizeanalyses.SizeAnalysis;
import com.uniovi.estimacion.entities.effortconversions.delphi.DelphiEstimation;
import com.uniovi.estimacion.entities.effortconversions.delphi.DelphiExpertEstimate;
import com.uniovi.estimacion.entities.effortconversions.delphi.DelphiIteration;
import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.services.sizeanalyses.SizeAnalysisModuleResult;
import com.uniovi.estimacion.services.sizeanalyses.SizeAnalysisProvider;
import com.uniovi.estimacion.services.sizeanalyses.SizeAnalysisProviderRegistry;
import com.uniovi.estimacion.services.costs.CostCalculationService;
import com.uniovi.estimacion.services.effortconversions.delphi.DelphiEstimationService;
import com.uniovi.estimacion.services.projects.EstimationProjectService;
import com.uniovi.estimacion.validators.effortconversions.delphi.DelphiEstimationValidator;
import com.uniovi.estimacion.validators.effortconversions.delphi.DelphiIterationValidator;
import com.uniovi.estimacion.web.forms.effortconversions.delphi.DelphiEstimationCreateForm;
import com.uniovi.estimacion.web.forms.effortconversions.delphi.DelphiExpertEstimateForm;
import com.uniovi.estimacion.web.forms.effortconversions.delphi.DelphiIterationForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/projects/{projectId}/size-analyses/{sourceTechniqueCode}/delphi")
@RequiredArgsConstructor
public class DelphiEstimationController {

    private final EstimationProjectService estimationProjectService;
    private final DelphiEstimationService delphiEstimationService;
    private final DelphiEstimationValidator delphiEstimationValidator;
    private final DelphiIterationValidator delphiIterationValidator;
    private final SizeAnalysisProviderRegistry sizeAnalysisProviderRegistry;
    private final CostCalculationService costCalculationService;

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

        List<SizeAnalysisModuleResult> moduleResults =
                getSourceAnalysisProvider(sourceTechniqueCode).buildModuleResults(analysis);

        delphiEstimationValidator.validate(createForm, result);

        if (!delphiEstimationService.canStartCalibration(moduleResults)) {
            result.reject("delphi.validation.notEnoughModules");
        }

        if (result.hasErrors()) {
            loadCreateFormModel(project, analysis, sourceTechniqueCode, createForm, model);
            return "effortconversions/delphi/add";
        }

        DelphiEstimation estimation = delphiEstimationService.createInitialEstimation(
                analysis,
                moduleResults,
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
                || !belongsToSourceAnalysis(optionalEstimation.get(), optionalAnalysis.get())) {
            return redirectToSourceAnalysisDetails(projectId, sourceTechniqueCode);
        }

        EstimationProject project = optionalProject.get();
        SizeAnalysis analysis = optionalAnalysis.get();
        DelphiEstimation estimation = optionalEstimation.get();

        List<SizeAnalysisModuleResult> moduleResults =
                getSourceAnalysisProvider(sourceTechniqueCode).buildModuleResults(analysis);

        Map<Long, Double> moduleEstimatedEffortById = new LinkedHashMap<>();
        boolean hasFinalCalibration = delphiEstimationService.isFinished(estimation);
        boolean canAddIteration = !hasFinalCalibration;
        Double totalEstimatedHours = null;
        BigDecimal totalEstimatedCost = null;

        if (hasFinalCalibration) {
            for (SizeAnalysisModuleResult moduleResult : moduleResults) {
                Double moduleSize = moduleResult.getSize();

                if (moduleSize != null && moduleSize > 0) {
                    moduleEstimatedEffortById.put(
                            moduleResult.getModuleId(),
                            delphiEstimationService.calculateEstimatedEffortHours(estimation, moduleSize)
                    );
                }
            }

            totalEstimatedHours =
                    delphiEstimationService.calculateTotalEstimatedEffortHours(estimation, moduleResults);


            if (totalEstimatedHours != null) {
                totalEstimatedCost =
                        costCalculationService.calculateCost(
                                totalEstimatedHours,
                                project.getHourlyRate()
                        );
            }
        }

        model.addAttribute("project", project);
        model.addAttribute("analysis", analysis);
        model.addAttribute("estimation", estimation);
        model.addAttribute("sourceTechniqueCode", sourceTechniqueCode);
        model.addAttribute("sourceAnalysisDetailsPath",
                getSourceAnalysisProvider(sourceTechniqueCode).getDetailsPath(projectId));
        model.addAttribute("moduleResults", moduleResults);
        model.addAttribute("moduleEstimatedEffortById", moduleEstimatedEffortById);
        model.addAttribute("totalEstimatedHours", totalEstimatedHours);
        model.addAttribute("totalEstimatedCost", totalEstimatedCost);
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
        Optional<? extends SizeAnalysis> optionalAnalysis =
                findSourceAnalysis(projectId, sourceTechniqueCode);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToSourceAnalysisAdd(projectId, sourceTechniqueCode);
        }

        if (optionalEstimation.isEmpty()
                || !belongsToSourceAnalysis(optionalEstimation.get(), optionalAnalysis.get())) {
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
        Optional<? extends SizeAnalysis> optionalAnalysis =
                findSourceAnalysis(projectId, sourceTechniqueCode);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToSourceAnalysisAdd(projectId, sourceTechniqueCode);
        }

        if (optionalEstimation.isEmpty()
                || !belongsToSourceAnalysis(optionalEstimation.get(), optionalAnalysis.get())) {
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
        Optional<? extends SizeAnalysis> optionalAnalysis =
                findSourceAnalysis(projectId, sourceTechniqueCode);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isPresent()
                && optionalEstimation.isPresent()
                && belongsToSourceAnalysis(optionalEstimation.get(), optionalAnalysis.get())) {
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
        Optional<? extends SizeAnalysis> optionalAnalysis =
                findSourceAnalysis(projectId, sourceTechniqueCode);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToSourceAnalysisAdd(projectId, sourceTechniqueCode);
        }

        if (optionalEstimation.isEmpty()
                || !belongsToSourceAnalysis(optionalEstimation.get(), optionalAnalysis.get())) {
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
        List<SizeAnalysisModuleResult> moduleResults =
                getSourceAnalysisProvider(analysis.getTechniqueCode()).buildModuleResults(analysis);

        model.addAttribute("project", project);
        model.addAttribute("analysis", analysis);
        model.addAttribute("sourceTechniqueCode", sourceTechniqueCode);
        model.addAttribute("sourceAnalysisDetailsPath",
                getSourceAnalysisProvider(sourceTechniqueCode).getDetailsPath(project.getId()));
        model.addAttribute("moduleResults", moduleResults);
        model.addAttribute("canStartDelphi", delphiEstimationService.canStartCalibration(moduleResults));
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

    private boolean belongsToSourceAnalysis(DelphiEstimation estimation, SizeAnalysis analysis) {
        return estimation != null
                && analysis != null
                && estimation.getSourceAnalysisId() != null
                && estimation.getSourceTechniqueCode() != null
                && estimation.getSourceAnalysisId().equals(analysis.getId())
                && estimation.getSourceTechniqueCode().equals(analysis.getTechniqueCode());
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