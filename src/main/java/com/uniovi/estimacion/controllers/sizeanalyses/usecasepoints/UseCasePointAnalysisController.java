package com.uniovi.estimacion.controllers.sizeanalyses.usecasepoints;

import com.uniovi.estimacion.entities.effortconversions.delphi.DelphiEstimation;
import com.uniovi.estimacion.entities.effortconversions.transformationfunctions.TransformationFunctionConversion;
import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.UseCasePointAnalysis;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.actors.UseCaseActor;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.actors.UseCaseActorComplexity;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.modules.UseCasePointModule;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.usecases.UseCaseEntry;
import com.uniovi.estimacion.services.sizeanalyses.SizeAnalysisModuleResult;
import com.uniovi.estimacion.services.sizeanalyses.usecasepoints.UseCasePointSizeAnalysisProvider;
import com.uniovi.estimacion.services.costs.CostCalculationService;
import com.uniovi.estimacion.services.effortconversions.delphi.DelphiEstimationService;
import com.uniovi.estimacion.services.effortconversions.transformationfunctions.TransformationFunctionService;
import com.uniovi.estimacion.services.projects.EstimationProjectService;
import com.uniovi.estimacion.services.sizeanalyses.usecasepoints.UseCasePointAnalysisService;
import com.uniovi.estimacion.services.sizeanalyses.usecasepoints.UseCasePointAnalysisSummary;
import com.uniovi.estimacion.services.sizeanalyses.usecasepoints.UseCasePointCalculationService;
import com.uniovi.estimacion.validators.sizeanalyses.usecasepoints.UseCaseActorValidator;
import com.uniovi.estimacion.validators.sizeanalyses.usecasepoints.UseCaseEntryValidator;
import com.uniovi.estimacion.validators.sizeanalyses.usecasepoints.UseCaseEnvironmentalFactorsValidator;
import com.uniovi.estimacion.validators.sizeanalyses.usecasepoints.UseCasePointAnalysisValidator;
import com.uniovi.estimacion.validators.sizeanalyses.usecasepoints.UseCasePointModuleValidator;
import com.uniovi.estimacion.validators.sizeanalyses.usecasepoints.UseCaseTechnicalFactorsValidator;
import com.uniovi.estimacion.web.forms.sizeanalyses.usecasepoints.UseCaseActorForm;
import com.uniovi.estimacion.web.forms.sizeanalyses.usecasepoints.UseCaseEntryForm;
import com.uniovi.estimacion.web.forms.sizeanalyses.usecasepoints.UseCaseEnvironmentalFactorsForm;
import com.uniovi.estimacion.web.forms.sizeanalyses.usecasepoints.UseCasePointAnalysisForm;
import com.uniovi.estimacion.web.forms.sizeanalyses.usecasepoints.UseCasePointModuleForm;
import com.uniovi.estimacion.web.forms.sizeanalyses.usecasepoints.UseCaseTechnicalFactorsForm;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
@RequestMapping("/projects/{projectId}/use-case-points")
@RequiredArgsConstructor
public class UseCasePointAnalysisController {

    private final EstimationProjectService estimationProjectService;
    private final UseCasePointAnalysisService useCasePointAnalysisService;
    private final UseCasePointCalculationService useCasePointCalculationService;

    private final UseCasePointAnalysisValidator useCasePointAnalysisValidator;
    private final UseCaseTechnicalFactorsValidator useCaseTechnicalFactorsValidator;
    private final UseCaseEnvironmentalFactorsValidator useCaseEnvironmentalFactorsValidator;
    private final UseCaseActorValidator useCaseActorValidator;
    private final UseCasePointModuleValidator useCasePointModuleValidator;
    private final UseCaseEntryValidator useCaseEntryValidator;

    private final DelphiEstimationService delphiEstimationService;
    private final TransformationFunctionService transformationFunctionService;
    private final CostCalculationService costCalculationService;
    private final UseCasePointSizeAnalysisProvider useCasePointSizeAnalysisProvider;

    private static final int UCP_PAGE_SIZE = 5;

    @GetMapping("/access")
    public String accessUseCasePointAnalysis(@PathVariable Long projectId) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisService.findDetailedByProjectId(projectId);

        if (optionalAnalysis.isPresent()) {
            return redirectToDetails(projectId);
        }

        return redirectToAdd(projectId);
    }

    @GetMapping("/add")
    public String getAddForm(@PathVariable Long projectId,
                             Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisService.findDetailedByProjectId(projectId);

        if (optionalAnalysis.isPresent()) {
            return redirectToDetails(projectId);
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("analysisForm", new UseCasePointAnalysisForm());

        return "ucp/add";
    }

    @PostMapping("/add")
    public String addAnalysis(@PathVariable Long projectId,
                              @ModelAttribute("analysisForm") UseCasePointAnalysisForm analysisForm,
                              BindingResult result,
                              Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisService.findDetailedByProjectId(projectId);

        if (optionalAnalysis.isPresent()) {
            return redirectToDetails(projectId);
        }

        useCasePointAnalysisValidator.validate(analysisForm, result);

        if (result.hasErrors()) {
            model.addAttribute("project", optionalProject.get());
            return "ucp/add";
        }

        useCasePointAnalysisService.createInitialAnalysis(
                optionalProject.get(),
                analysisForm.getSystemBoundaryDescription()
        );

        return redirectToDetails(projectId);
    }

    @GetMapping
    public String getDetails(@PathVariable Long projectId,
                             @RequestParam(name = "actorsPage", defaultValue = "0") int actorsPageNumber,
                             @RequestParam(name = "modulesPage", defaultValue = "0") int modulesPageNumber,
                             Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisService.findDetailedByProjectId(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToAdd(projectId);
        }

        EstimationProject project = optionalProject.get();
        UseCasePointAnalysis analysis = optionalAnalysis.get();

        UseCasePointAnalysisSummary results =
                useCasePointCalculationService.buildSummary(analysis);

        Page<UseCaseActor> actorsPage =
                useCasePointAnalysisService.findActorsPageByProjectId(
                        projectId,
                        buildPageable(actorsPageNumber)
                );

        Page<UseCasePointModule> modulesPage =
                useCasePointAnalysisService.findModulesPageByProjectId(
                        projectId,
                        buildPageable(modulesPageNumber)
                );

        Map<Long, Integer> moduleUseCaseWeightById = new LinkedHashMap<>();

        for (UseCasePointModule module : modulesPage.getContent()) {
            int totalWeight = module.getUseCases().stream()
                    .mapToInt(useCase -> useCase.getWeight() != null ? useCase.getWeight() : 0)
                    .sum();

            moduleUseCaseWeightById.put(module.getId(), totalWeight);
        }


        Map<Long, Double> moduleUnadjustedSizeById = new LinkedHashMap<>();
        Map<Long, Double> moduleAdjustedSizeById = new LinkedHashMap<>();

        for (UseCasePointModule module : analysis.getModules()) {
            moduleUnadjustedSizeById.put(
                    module.getId(),
                    useCasePointCalculationService.calculateUnadjustedUseCasePointsForModule(
                            analysis,
                            module.getUseCases()
                    )
            );

            moduleAdjustedSizeById.put(
                    module.getId(),
                    useCasePointCalculationService.calculateAdjustedUseCasePointsForModule(
                            analysis,
                            module.getUseCases()
                    )
            );
        }

        DelphiEstimation activeUseCasePointDelphi = null;
        Double useCasePointDelphiEstimatedTotalHours = null;
        BigDecimal useCasePointDelphiEstimatedCost = null;

        TransformationFunctionConversion activeUseCasePointTransformationConversion = null;
        Double useCasePointTransformationEstimatedHours = null;
        BigDecimal useCasePointTransformationEstimatedCost = null;

        Integer activeUseCasePointDelphiIterationsCount = 0;

        Optional<DelphiEstimation> optionalActiveDelphi =
                delphiEstimationService.findDetailedActiveBySourceAnalysis(analysis);

        if (optionalActiveDelphi.isPresent()) {
            activeUseCasePointDelphi = optionalActiveDelphi.get();
            activeUseCasePointDelphiIterationsCount =
                    activeUseCasePointDelphi.getIterations() != null
                            ? activeUseCasePointDelphi.getIterations().size()
                            : 0;

            if (activeUseCasePointDelphi.getRegressionIntercept() != null
                    && activeUseCasePointDelphi.getRegressionSlope() != null) {

                List<SizeAnalysisModuleResult> moduleResults =
                        useCasePointSizeAnalysisProvider.buildModuleResults(analysis);

                useCasePointDelphiEstimatedTotalHours =
                        delphiEstimationService.calculateTotalEstimatedEffortHours(
                                activeUseCasePointDelphi,
                                moduleResults
                        );

                useCasePointDelphiEstimatedCost =
                        costCalculationService.calculateCost(
                                useCasePointDelphiEstimatedTotalHours,
                                project.getHourlyRate()
                        );
            }
        }

        Optional<TransformationFunctionConversion> optionalActiveTransformationConversion =
                transformationFunctionService.findActiveBySourceAnalysis(analysis);

        if (optionalActiveTransformationConversion.isPresent()) {
            activeUseCasePointTransformationConversion = optionalActiveTransformationConversion.get();

            useCasePointTransformationEstimatedHours =
                    transformationFunctionService.calculateEstimatedEffortHours(
                            activeUseCasePointTransformationConversion,
                            analysis.getCalculatedSizeValue()
                    );

            useCasePointTransformationEstimatedCost =
                    costCalculationService.calculateCost(
                            useCasePointTransformationEstimatedHours,
                            project.getHourlyRate()
                    );
        }

        model.addAttribute("project", project);
        model.addAttribute("analysis", analysis);
        model.addAttribute("results", results);

        model.addAttribute("moduleUnadjustedSizeById", moduleUnadjustedSizeById);
        model.addAttribute("moduleAdjustedSizeById", moduleAdjustedSizeById);

        model.addAttribute("actorsList", actorsPage.getContent());
        model.addAttribute("actorsPage", actorsPage);

        model.addAttribute("modulesList", modulesPage.getContent());
        model.addAttribute("modulesPage", modulesPage);
        model.addAttribute("moduleUseCaseWeightById", moduleUseCaseWeightById);

        model.addAttribute("technicalFactorsList", analysis.getTechnicalFactorAssessments());
        model.addAttribute("environmentalFactorsList", analysis.getEnvironmentalFactorAssessments());
        model.addAttribute("sourceTechniqueCode", analysis.getTechniqueCode());

        model.addAttribute("activeUseCasePointDelphi", activeUseCasePointDelphi);
        model.addAttribute("useCasePointDelphiEstimatedTotalHours", useCasePointDelphiEstimatedTotalHours);
        model.addAttribute("useCasePointDelphiEstimatedCost", useCasePointDelphiEstimatedCost);
        model.addAttribute("activeUseCasePointDelphiIterationsCount", activeUseCasePointDelphiIterationsCount);

        model.addAttribute("activeUseCasePointTransformationConversion", activeUseCasePointTransformationConversion);
        model.addAttribute("useCasePointTransformationEstimatedHours", useCasePointTransformationEstimatedHours);
        model.addAttribute("useCasePointTransformationEstimatedCost", useCasePointTransformationEstimatedCost);

        return "ucp/details";
    }

    @GetMapping("/edit")
    public String getEditForm(@PathVariable Long projectId,
                              Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisService.findDetailedByProjectId(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToAdd(projectId);
        }

        UseCasePointAnalysis analysis = optionalAnalysis.get();

        UseCasePointAnalysisForm analysisForm = new UseCasePointAnalysisForm();
        analysisForm.setSystemBoundaryDescription(analysis.getSystemBoundaryDescription());

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("analysis", analysis);
        model.addAttribute("analysisForm", analysisForm);

        return "ucp/edit";
    }

    @PostMapping("/edit")
    public String editAnalysis(@PathVariable Long projectId,
                               @ModelAttribute("analysisForm") UseCasePointAnalysisForm analysisForm,
                               BindingResult result,
                               Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisService.findDetailedByProjectId(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToAdd(projectId);
        }

        useCasePointAnalysisValidator.validate(analysisForm, result);

        if (result.hasErrors()) {
            model.addAttribute("project", optionalProject.get());
            model.addAttribute("analysis", optionalAnalysis.get());
            return "ucp/edit";
        }

        useCasePointAnalysisService.updateSystemBoundaryDescription(
                projectId,
                analysisForm.getSystemBoundaryDescription()
        );

        return redirectToDetails(projectId);
    }

    @GetMapping("/modules/add")
    public String getAddModuleForm(@PathVariable Long projectId,
                                   Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisService.findDetailedByProjectId(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToAdd(projectId);
        }

        loadModuleFormModel(
                optionalProject.get(),
                optionalAnalysis.get(),
                new UseCasePointModuleForm(),
                model
        );

        return "ucp/modules/add";
    }

    @PostMapping("/modules/add")
    public String addModule(@PathVariable Long projectId,
                            @ModelAttribute("moduleForm") UseCasePointModuleForm moduleForm,
                            BindingResult result,
                            Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisService.findDetailedByProjectId(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToAdd(projectId);
        }

        useCasePointModuleValidator.validate(moduleForm, result);

        if (result.hasErrors()) {
            loadModuleFormModel(optionalProject.get(), optionalAnalysis.get(), moduleForm, model);
            return "ucp/modules/add";
        }

        Optional<UseCasePointModule> createdModule =
                useCasePointAnalysisService.addModule(
                        projectId,
                        toModuleEntity(moduleForm)
                );

        if (createdModule.isEmpty()) {
            return redirectToDetails(projectId);
        }

        return redirectToModuleDetails(projectId, createdModule.get().getId());
    }

    @GetMapping("/modules/{moduleId}")
    public String getModuleDetails(@PathVariable Long projectId,
                                   @PathVariable Long moduleId,
                                   @RequestParam(name = "useCasesPage", defaultValue = "0") int useCasesPageNumber,
                                   Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisService.findDetailedByProjectId(projectId);
        Optional<UseCasePointModule> optionalModule =
                useCasePointAnalysisService.findModule(projectId, moduleId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToAdd(projectId);
        }

        if (optionalModule.isEmpty()) {
            return redirectToDetails(projectId);
        }

        UseCasePointAnalysis analysis = optionalAnalysis.get();
        UseCasePointModule module = optionalModule.get();

        Page<UseCaseEntry> useCasesPage =
                useCasePointAnalysisService.findUseCasesPageByModuleId(
                        moduleId,
                        buildPageable(useCasesPageNumber)
                );

        double moduleUnadjustedSize =
                useCasePointCalculationService.calculateUnadjustedUseCasePointsForModule(
                        analysis,
                        module.getUseCases()
                );

        double moduleAdjustedSize =
                useCasePointCalculationService.calculateAdjustedUseCasePointsForModule(
                        analysis,
                        module.getUseCases()
                );

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("analysis", analysis);
        model.addAttribute("module", module);
        model.addAttribute("useCasesList", useCasesPage.getContent());
        model.addAttribute("useCasesPage", useCasesPage);
        model.addAttribute("moduleUnadjustedSize", moduleUnadjustedSize);
        model.addAttribute("moduleAdjustedSize", moduleAdjustedSize);

        return "ucp/modules/details";
    }

    @GetMapping("/modules/{moduleId}/edit")
    public String getEditModuleForm(@PathVariable Long projectId,
                                    @PathVariable Long moduleId,
                                    Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisService.findDetailedByProjectId(projectId);
        Optional<UseCasePointModule> optionalModule =
                useCasePointAnalysisService.findModule(projectId, moduleId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToAdd(projectId);
        }

        if (optionalModule.isEmpty()) {
            return redirectToDetails(projectId);
        }

        loadModuleFormModel(
                optionalProject.get(),
                optionalAnalysis.get(),
                toModuleForm(optionalModule.get()),
                model
        );

        model.addAttribute("module", optionalModule.get());

        return "ucp/modules/edit";
    }

    @PostMapping("/modules/{moduleId}/edit")
    public String editModule(@PathVariable Long projectId,
                             @PathVariable Long moduleId,
                             @ModelAttribute("moduleForm") UseCasePointModuleForm moduleForm,
                             BindingResult result,
                             Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisService.findDetailedByProjectId(projectId);
        Optional<UseCasePointModule> optionalModule =
                useCasePointAnalysisService.findModule(projectId, moduleId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToAdd(projectId);
        }

        if (optionalModule.isEmpty()) {
            return redirectToDetails(projectId);
        }

        useCasePointModuleValidator.validate(moduleForm, result);

        if (result.hasErrors()) {
            loadModuleFormModel(optionalProject.get(), optionalAnalysis.get(), moduleForm, model);
            model.addAttribute("module", optionalModule.get());
            return "ucp/modules/edit";
        }

        useCasePointAnalysisService.updateModule(
                projectId,
                moduleId,
                toModuleEntity(moduleForm)
        );

        return redirectToModuleDetails(projectId, moduleId);
    }

    @GetMapping("/modules/{moduleId}/delete")
    public String deleteModule(@PathVariable Long projectId,
                               @PathVariable Long moduleId) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        useCasePointAnalysisService.deleteModule(projectId, moduleId);

        return redirectToDetails(projectId);
    }

    @GetMapping("/actors/add")
    public String getAddActorForm(@PathVariable Long projectId,
                                  Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisService.findDetailedByProjectId(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToAdd(projectId);
        }

        loadActorFormModel(
                optionalProject.get(),
                optionalAnalysis.get(),
                new UseCaseActorForm(),
                model
        );

        return "ucp/actors/add";
    }

    @PostMapping("/actors/add")
    public String addActor(@PathVariable Long projectId,
                           @ModelAttribute("actorForm") UseCaseActorForm actorForm,
                           BindingResult result,
                           Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisService.findDetailedByProjectId(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToAdd(projectId);
        }

        useCaseActorValidator.validate(actorForm, result);

        if (result.hasErrors()) {
            loadActorFormModel(optionalProject.get(), optionalAnalysis.get(), actorForm, model);
            return "ucp/actors/add";
        }

        useCasePointAnalysisService.addActor(projectId, toActorEntity(actorForm));

        return redirectToDetails(projectId);
    }

    @GetMapping("/actors/{actorId}/edit")
    public String getEditActorForm(@PathVariable Long projectId,
                                   @PathVariable Long actorId,
                                   Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisService.findDetailedByProjectId(projectId);
        Optional<UseCaseActor> optionalActor =
                useCasePointAnalysisService.findActor(projectId, actorId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToAdd(projectId);
        }

        if (optionalActor.isEmpty()) {
            return redirectToDetails(projectId);
        }

        loadActorFormModel(
                optionalProject.get(),
                optionalAnalysis.get(),
                toActorForm(optionalActor.get()),
                model
        );

        model.addAttribute("actor", optionalActor.get());

        return "ucp/actors/edit";
    }

    @PostMapping("/actors/{actorId}/edit")
    public String editActor(@PathVariable Long projectId,
                            @PathVariable Long actorId,
                            @ModelAttribute("actorForm") UseCaseActorForm actorForm,
                            BindingResult result,
                            Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisService.findDetailedByProjectId(projectId);
        Optional<UseCaseActor> optionalActor =
                useCasePointAnalysisService.findActor(projectId, actorId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToAdd(projectId);
        }

        if (optionalActor.isEmpty()) {
            return redirectToDetails(projectId);
        }

        useCaseActorValidator.validate(actorForm, result);

        if (result.hasErrors()) {
            loadActorFormModel(optionalProject.get(), optionalAnalysis.get(), actorForm, model);
            model.addAttribute("actor", optionalActor.get());
            return "ucp/actors/edit";
        }

        useCasePointAnalysisService.updateActor(
                projectId,
                actorId,
                toActorEntity(actorForm)
        );

        return redirectToDetails(projectId);
    }

    @GetMapping("/actors/{actorId}/delete")
    public String deleteActor(@PathVariable Long projectId,
                              @PathVariable Long actorId) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        useCasePointAnalysisService.deleteActor(projectId, actorId);

        return redirectToDetails(projectId);
    }

    @GetMapping("/modules/{moduleId}/use-cases/add")
    public String getAddUseCaseForm(@PathVariable Long projectId,
                                    @PathVariable Long moduleId,
                                    Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisService.findDetailedByProjectId(projectId);
        Optional<UseCasePointModule> optionalModule =
                useCasePointAnalysisService.findModule(projectId, moduleId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToAdd(projectId);
        }

        if (optionalModule.isEmpty()) {
            return redirectToDetails(projectId);
        }

        loadUseCaseFormModel(
                optionalProject.get(),
                optionalAnalysis.get(),
                optionalModule.get(),
                new UseCaseEntryForm(),
                model
        );

        return "ucp/usecases/add";
    }

    @PostMapping("/modules/{moduleId}/use-cases/add")
    public String addUseCase(@PathVariable Long projectId,
                             @PathVariable Long moduleId,
                             @ModelAttribute("useCaseForm") UseCaseEntryForm useCaseForm,
                             BindingResult result,
                             Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisService.findDetailedByProjectId(projectId);
        Optional<UseCasePointModule> optionalModule =
                useCasePointAnalysisService.findModule(projectId, moduleId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToAdd(projectId);
        }

        if (optionalModule.isEmpty()) {
            return redirectToDetails(projectId);
        }

        useCaseEntryValidator.validate(useCaseForm, result);

        if (result.hasErrors()) {
            loadUseCaseFormModel(
                    optionalProject.get(),
                    optionalAnalysis.get(),
                    optionalModule.get(),
                    useCaseForm,
                    model
            );
            return "ucp/usecases/add";
        }

        useCasePointAnalysisService.addUseCaseToModule(
                projectId,
                moduleId,
                toUseCaseEntity(useCaseForm),
                useCaseForm.getActorIds()
        );

        return redirectToModuleDetails(projectId, moduleId);
    }

    @GetMapping("/modules/{moduleId}/use-cases/{useCaseId}/edit")
    public String getEditUseCaseForm(@PathVariable Long projectId,
                                     @PathVariable Long moduleId,
                                     @PathVariable Long useCaseId,
                                     Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisService.findDetailedByProjectId(projectId);
        Optional<UseCasePointModule> optionalModule =
                useCasePointAnalysisService.findModule(projectId, moduleId);
        Optional<UseCaseEntry> optionalUseCase =
                useCasePointAnalysisService.findUseCase(projectId, useCaseId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToAdd(projectId);
        }

        if (optionalModule.isEmpty() || optionalUseCase.isEmpty()
                || !belongsToModule(optionalUseCase.get(), moduleId)) {
            return redirectToModuleDetails(projectId, moduleId);
        }

        loadUseCaseFormModel(
                optionalProject.get(),
                optionalAnalysis.get(),
                optionalModule.get(),
                toUseCaseForm(optionalUseCase.get()),
                model
        );

        model.addAttribute("useCase", optionalUseCase.get());

        return "ucp/usecases/edit";
    }

    @PostMapping("/modules/{moduleId}/use-cases/{useCaseId}/edit")
    public String editUseCase(@PathVariable Long projectId,
                              @PathVariable Long moduleId,
                              @PathVariable Long useCaseId,
                              @ModelAttribute("useCaseForm") UseCaseEntryForm useCaseForm,
                              BindingResult result,
                              Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisService.findDetailedByProjectId(projectId);
        Optional<UseCasePointModule> optionalModule =
                useCasePointAnalysisService.findModule(projectId, moduleId);
        Optional<UseCaseEntry> optionalUseCase =
                useCasePointAnalysisService.findUseCase(projectId, useCaseId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToAdd(projectId);
        }

        if (optionalModule.isEmpty() || optionalUseCase.isEmpty()
                || !belongsToModule(optionalUseCase.get(), moduleId)) {
            return redirectToModuleDetails(projectId, moduleId);
        }

        useCaseEntryValidator.validate(useCaseForm, result);

        if (result.hasErrors()) {
            loadUseCaseFormModel(
                    optionalProject.get(),
                    optionalAnalysis.get(),
                    optionalModule.get(),
                    useCaseForm,
                    model
            );
            model.addAttribute("useCase", optionalUseCase.get());
            return "ucp/usecases/edit";
        }

        useCasePointAnalysisService.updateUseCase(
                projectId,
                moduleId,
                useCaseId,
                toUseCaseEntity(useCaseForm),
                useCaseForm.getActorIds()
        );
        return redirectToModuleDetails(projectId, moduleId);
    }

    @GetMapping("/modules/{moduleId}/use-cases/{useCaseId}/delete")
    public String deleteUseCase(@PathVariable Long projectId,
                                @PathVariable Long moduleId,
                                @PathVariable Long useCaseId) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        useCasePointAnalysisService.deleteUseCase(projectId, moduleId, useCaseId);

        return redirectToModuleDetails(projectId, moduleId);
    }

    @GetMapping("/technical-factors/edit")
    public String getEditTechnicalFactorsForm(@PathVariable Long projectId,
                                              Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisService.findDetailedByProjectId(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToAdd(projectId);
        }

        UseCasePointAnalysis analysis = optionalAnalysis.get();
        UseCaseTechnicalFactorsForm technicalFactorsForm =
                useCasePointAnalysisService.buildTechnicalFactorsForm(analysis);

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("analysis", analysis);
        model.addAttribute("technicalFactorsList", analysis.getTechnicalFactorAssessments());
        model.addAttribute("technicalFactorsForm", technicalFactorsForm);

        return "ucp/factors/technical-factors-edit";
    }

    @PostMapping("/technical-factors/edit")
    public String editTechnicalFactors(@PathVariable Long projectId,
                                       @ModelAttribute("technicalFactorsForm") UseCaseTechnicalFactorsForm technicalFactorsForm,
                                       BindingResult result,
                                       Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisService.findDetailedByProjectId(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToAdd(projectId);
        }

        UseCasePointAnalysis analysis = optionalAnalysis.get();

        useCaseTechnicalFactorsValidator.validate(technicalFactorsForm, result);

        if (result.hasErrors()) {
            model.addAttribute("project", optionalProject.get());
            model.addAttribute("analysis", analysis);
            model.addAttribute("technicalFactorsList", analysis.getTechnicalFactorAssessments());
            return "ucp/factors/technical-factors-edit";
        }

        useCasePointAnalysisService.updateTechnicalFactors(projectId, technicalFactorsForm);

        return redirectToDetails(projectId);
    }

    @GetMapping("/environmental-factors/edit")
    public String getEditEnvironmentalFactorsForm(@PathVariable Long projectId,
                                                  Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisService.findDetailedByProjectId(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToAdd(projectId);
        }

        UseCasePointAnalysis analysis = optionalAnalysis.get();
        UseCaseEnvironmentalFactorsForm environmentalFactorsForm =
                useCasePointAnalysisService.buildEnvironmentalFactorsForm(analysis);

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("analysis", analysis);
        model.addAttribute("environmentalFactorsList", analysis.getEnvironmentalFactorAssessments());
        model.addAttribute("environmentalFactorsForm", environmentalFactorsForm);

        return "ucp/factors/environmental-factors-edit";
    }

    @PostMapping("/environmental-factors/edit")
    public String editEnvironmentalFactors(@PathVariable Long projectId,
                                           @ModelAttribute("environmentalFactorsForm") UseCaseEnvironmentalFactorsForm environmentalFactorsForm,
                                           BindingResult result,
                                           Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisService.findDetailedByProjectId(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToAdd(projectId);
        }

        UseCasePointAnalysis analysis = optionalAnalysis.get();

        useCaseEnvironmentalFactorsValidator.validate(environmentalFactorsForm, result);

        if (result.hasErrors()) {
            model.addAttribute("project", optionalProject.get());
            model.addAttribute("analysis", analysis);
            model.addAttribute("environmentalFactorsList", analysis.getEnvironmentalFactorAssessments());
            return "ucp/factors/environmental-factors-edit";
        }

        useCasePointAnalysisService.updateEnvironmentalFactors(projectId, environmentalFactorsForm);

        return redirectToDetails(projectId);
    }

    @GetMapping("/delete")
    public String deleteAnalysis(@PathVariable Long projectId) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        useCasePointAnalysisService.deleteByProjectId(projectId);

        return redirectToProjectDetails(projectId);
    }

    @GetMapping("/modules/{moduleId}/use-cases/{useCaseId}")
    public String getUseCaseDetails(@PathVariable Long projectId,
                                    @PathVariable Long moduleId,
                                    @PathVariable Long useCaseId,
                                    Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisService.findDetailedByProjectId(projectId);
        Optional<UseCasePointModule> optionalModule =
                useCasePointAnalysisService.findModule(projectId, moduleId);
        Optional<UseCaseEntry> optionalUseCase =
                useCasePointAnalysisService.findUseCase(projectId, useCaseId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToAdd(projectId);
        }

        if (optionalModule.isEmpty()) {
            return redirectToDetails(projectId);
        }

        if (optionalUseCase.isEmpty() || !belongsToModule(optionalUseCase.get(), moduleId)) {
            return redirectToModuleDetails(projectId, moduleId);
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("analysis", optionalAnalysis.get());
        model.addAttribute("module", optionalModule.get());
        model.addAttribute("useCase", optionalUseCase.get());

        return "ucp/usecases/details";
    }

    @GetMapping("/actors/update")
    public String updateActorsSection(@PathVariable Long projectId,
                                      @RequestParam(name = "actorsPage", defaultValue = "0") int actorsPageNumber,
                                      @RequestParam(name = "modulesPage", defaultValue = "0") int modulesPageNumber,
                                      Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        Page<UseCaseActor> actorsPage =
                useCasePointAnalysisService.findActorsPageByProjectId(
                        projectId,
                        buildPageable(actorsPageNumber)
                );

        Page<UseCasePointModule> modulesPage =
                useCasePointAnalysisService.findModulesPageByProjectId(
                        projectId,
                        buildPageable(modulesPageNumber)
                );

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("actorsList", actorsPage.getContent());
        model.addAttribute("actorsPage", actorsPage);
        model.addAttribute("modulesPage", modulesPage);

        return "ucp/details :: actorsSection";
    }

    @GetMapping("/modules/update")
    public String updateModulesSection(@PathVariable Long projectId,
                                       @RequestParam(name = "actorsPage", defaultValue = "0") int actorsPageNumber,
                                       @RequestParam(name = "modulesPage", defaultValue = "0") int modulesPageNumber,
                                       Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        Page<UseCaseActor> actorsPage =
                useCasePointAnalysisService.findActorsPageByProjectId(
                        projectId,
                        buildPageable(actorsPageNumber)
                );

        Page<UseCasePointModule> modulesPage =
                useCasePointAnalysisService.findModulesPageByProjectId(
                        projectId,
                        buildPageable(modulesPageNumber)
                );


        Map<Long, Double> moduleUnadjustedSizeById = new LinkedHashMap<>();
        Map<Long, Double> moduleAdjustedSizeById = new LinkedHashMap<>();

        UseCasePointAnalysis analysis = useCasePointAnalysisService
                .findDetailedByProjectId(projectId)
                .orElseThrow();

        for (UseCasePointModule module : modulesPage.getContent()) {
            moduleUnadjustedSizeById.put(
                    module.getId(),
                    useCasePointCalculationService.calculateUnadjustedUseCasePointsForModule(
                            analysis,
                            module.getUseCases()
                    )
            );

            moduleAdjustedSizeById.put(
                    module.getId(),
                    useCasePointCalculationService.calculateAdjustedUseCasePointsForModule(
                            analysis,
                            module.getUseCases()
                    )
            );
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("actorsPage", actorsPage);
        model.addAttribute("modulesList", modulesPage.getContent());
        model.addAttribute("modulesPage", modulesPage);
        model.addAttribute("moduleUnadjustedSizeById", moduleUnadjustedSizeById);
        model.addAttribute("moduleAdjustedSizeById", moduleAdjustedSizeById);

        return "ucp/details :: modulesSection";
    }

    @GetMapping("/modules/{moduleId}/use-cases/update")
    public String updateUseCasesSection(@PathVariable Long projectId,
                                        @PathVariable Long moduleId,
                                        @RequestParam(name = "useCasesPage", defaultValue = "0") int useCasesPageNumber,
                                        Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);
        Optional<UseCasePointModule> optionalModule =
                useCasePointAnalysisService.findModule(projectId, moduleId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalModule.isEmpty()) {
            return redirectToDetails(projectId);
        }

        Page<UseCaseEntry> useCasesPage =
                useCasePointAnalysisService.findUseCasesPageByModuleId(
                        moduleId,
                        buildPageable(useCasesPageNumber)
                );

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("module", optionalModule.get());
        model.addAttribute("useCasesList", useCasesPage.getContent());
        model.addAttribute("useCasesPage", useCasesPage);

        return "ucp/modules/details :: useCasesSection";
    }

    private void loadModuleFormModel(EstimationProject project,
                                     UseCasePointAnalysis analysis,
                                     UseCasePointModuleForm moduleForm,
                                     Model model) {
        model.addAttribute("project", project);
        model.addAttribute("analysis", analysis);
        model.addAttribute("moduleForm", moduleForm);
    }

    private void loadActorFormModel(EstimationProject project,
                                    UseCasePointAnalysis analysis,
                                    UseCaseActorForm actorForm,
                                    Model model) {
        model.addAttribute("project", project);
        model.addAttribute("analysis", analysis);
        model.addAttribute("actorForm", actorForm);
        model.addAttribute("actorComplexities", UseCaseActorComplexity.values());
    }

    private void loadUseCaseFormModel(EstimationProject project,
                                      UseCasePointAnalysis analysis,
                                      UseCasePointModule module,
                                      UseCaseEntryForm useCaseForm,
                                      Model model) {
        model.addAttribute("project", project);
        model.addAttribute("analysis", analysis);
        model.addAttribute("module", module);
        model.addAttribute("useCaseForm", useCaseForm);
        model.addAttribute("actorsList", analysis.getActors());
    }

    private UseCasePointModule toModuleEntity(UseCasePointModuleForm form) {
        UseCasePointModule module = new UseCasePointModule();
        module.setName(form.getName());
        module.setDescription(form.getDescription());
        return module;
    }

    private UseCasePointModuleForm toModuleForm(UseCasePointModule module) {
        UseCasePointModuleForm form = new UseCasePointModuleForm();
        form.setName(module.getName());
        form.setDescription(module.getDescription());
        return form;
    }

    private UseCaseActor toActorEntity(UseCaseActorForm form) {
        UseCaseActor actor = new UseCaseActor();
        actor.setName(form.getName());
        actor.setDescription(form.getDescription());
        actor.setComplexity(form.getComplexity());
        return actor;
    }

    private UseCaseActorForm toActorForm(UseCaseActor actor) {
        UseCaseActorForm form = new UseCaseActorForm();
        form.setName(actor.getName());
        form.setDescription(actor.getDescription());
        form.setComplexity(actor.getComplexity());
        return form;
    }

    private UseCaseEntry toUseCaseEntity(UseCaseEntryForm form) {
        UseCaseEntry useCase = new UseCaseEntry();
        useCase.setName(form.getName());
        useCase.setDescription(form.getDescription());
        useCase.setTriggerCondition(form.getTriggerCondition());
        useCase.setPreconditions(form.getPreconditions());
        useCase.setPostconditions(form.getPostconditions());
        useCase.setNormalFlow(form.getNormalFlow());
        useCase.setAlternativeFlows(form.getAlternativeFlows());
        useCase.setExceptionFlows(form.getExceptionFlows());
        useCase.setTransactionCount(form.getTransactionCount());
        return useCase;
    }

    private UseCaseEntryForm toUseCaseForm(UseCaseEntry useCase) {
        UseCaseEntryForm form = new UseCaseEntryForm();
        form.setName(useCase.getName());
        form.setDescription(useCase.getDescription());
        form.setTriggerCondition(useCase.getTriggerCondition());
        form.setPreconditions(useCase.getPreconditions());
        form.setPostconditions(useCase.getPostconditions());
        form.setNormalFlow(useCase.getNormalFlow());
        form.setAlternativeFlows(useCase.getAlternativeFlows());
        form.setExceptionFlows(useCase.getExceptionFlows());
        form.setTransactionCount(useCase.getTransactionCount());

        form.setActorIds(
                useCase.getActors().stream()
                        .map(UseCaseActor::getId)
                        .toList()
        );

        return form;
    }

    private boolean belongsToModule(UseCaseEntry useCase, Long moduleId) {
        return useCase.getUseCasePointModule() != null
                && useCase.getUseCasePointModule().getId() != null
                && useCase.getUseCasePointModule().getId().equals(moduleId);
    }

    private Pageable buildPageable(int pageNumber) {
        int safePageNumber = Math.max(pageNumber, 0);
        return PageRequest.of(safePageNumber, UCP_PAGE_SIZE);
    }

    private String redirectToProjects() {
        return "redirect:/projects";
    }

    private String redirectToProjectDetails(Long projectId) {
        return "redirect:/projects/" + projectId;
    }

    private String redirectToAdd(Long projectId) {
        return "redirect:/projects/" + projectId + "/use-case-points/add";
    }

    private String redirectToDetails(Long projectId) {
        return "redirect:/projects/" + projectId + "/use-case-points";
    }

    private String redirectToModuleDetails(Long projectId, Long moduleId) {
        return "redirect:/projects/" + projectId + "/use-case-points/modules/" + moduleId;
    }
}