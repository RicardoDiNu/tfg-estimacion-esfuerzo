package com.uniovi.estimacion.controllers;

import com.uniovi.estimacion.entities.functionpoints.DataFunction;
import com.uniovi.estimacion.entities.functionpoints.DataFunctionType;
import com.uniovi.estimacion.entities.functionpoints.FunctionPointAnalysis;
import com.uniovi.estimacion.entities.functionpoints.TransactionalFunction;
import com.uniovi.estimacion.entities.functionpoints.TransactionalFunctionType;
import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.requirements.UserRequirement;
import com.uniovi.estimacion.services.functionpoints.FunctionPointAnalysisService;
import com.uniovi.estimacion.services.projects.EstimationProjectService;
import com.uniovi.estimacion.services.requirements.UserRequirementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/projects/{projectId}")
@RequiredArgsConstructor
public class FunctionPointAnalysisController {

    private final EstimationProjectService estimationProjectService;
    private final FunctionPointAnalysisService functionPointAnalysisService;
    private final UserRequirementService userRequirementService;

    @GetMapping("/function-points/add")
    public String getCreateForm(@PathVariable Long projectId, Model model) {
        Optional<EstimationProject> optionalProject = estimationProjectService.findById(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (functionPointAnalysisService.findByProjectId(projectId).isPresent()) {
            return redirectToFunctionPointDetails(projectId);
        }

        model.addAttribute("project", optionalProject.get());
        return "fp/add";
    }

    @PostMapping("/function-points/add")
    public String createAnalysis(@PathVariable Long projectId,
                                 @RequestParam("systemBoundaryDescription") String systemBoundaryDescription) {
        Optional<EstimationProject> optionalProject = estimationProjectService.findById(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (functionPointAnalysisService.findByProjectId(projectId).isPresent()) {
            return redirectToFunctionPointDetails(projectId);
        }

        functionPointAnalysisService.createInitialAnalysis(optionalProject.get(), systemBoundaryDescription);
        return redirectToFunctionPointDetails(projectId);
    }

    @GetMapping("/function-points")
    public String getFunctionPointAnalysisDetails(@PathVariable Long projectId,
                                                  @RequestParam(name = "requirementsPage", defaultValue = "0") int requirementsPage,
                                                  @RequestParam(name = "dataFunctionsPage", defaultValue = "0") int dataFunctionsPage,
                                                  @RequestParam(name = "transactionalFunctionsPage", defaultValue = "0") int transactionalFunctionsPage,
                                                  Model model) {
        Optional<EstimationProject> optionalProject = estimationProjectService.findById(projectId);
        Optional<FunctionPointAnalysis> optionalAnalysis = functionPointAnalysisService.findDetailedByProjectId(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToFunctionPointAdd(projectId);
        }

        Page<UserRequirement> requirementsPageResult =
                userRequirementService.findPageByProjectId(projectId, PageRequest.of(requirementsPage, 5));

        Page<DataFunction> dataFunctionsPageResult =
                functionPointAnalysisService.findDataFunctionsPageByProjectId(projectId, PageRequest.of(dataFunctionsPage, 5));

        Page<TransactionalFunction> transactionalFunctionsPageResult =
                functionPointAnalysisService.findTransactionalFunctionsPageByProjectId(projectId, PageRequest.of(transactionalFunctionsPage, 5));

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("analysis", optionalAnalysis.get());

        model.addAttribute("requirementsList", requirementsPageResult.getContent());
        model.addAttribute("requirementsPage", requirementsPageResult);

        model.addAttribute("dataFunctionsList", dataFunctionsPageResult.getContent());
        model.addAttribute("dataFunctionsPage", dataFunctionsPageResult);

        model.addAttribute("transactionalFunctionsList", transactionalFunctionsPageResult.getContent());
        model.addAttribute("transactionalFunctionsPage", transactionalFunctionsPageResult);

        model.addAttribute("dataFunctionsCurrentPage", dataFunctionsPageResult.getNumber());
        model.addAttribute("transactionalFunctionsCurrentPage", transactionalFunctionsPageResult.getNumber());

        return "fp/details";
    }

    @GetMapping("/function-points/edit")
    public String getEditForm(@PathVariable Long projectId, Model model) {
        Optional<EstimationProject> optionalProject = estimationProjectService.findById(projectId);
        Optional<FunctionPointAnalysis> optionalAnalysis = functionPointAnalysisService.findByProjectId(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToFunctionPointAdd(projectId);
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("analysis", optionalAnalysis.get());
        model.addAttribute("systemBoundaryDescription", optionalAnalysis.get().getSystemBoundaryDescription());

        return "fp/edit";
    }

    @PostMapping("/function-points/edit")
    public String updateAnalysis(@PathVariable Long projectId,
                                 @RequestParam("systemBoundaryDescription") String systemBoundaryDescription,
                                 Model model) {
        Optional<EstimationProject> optionalProject = estimationProjectService.findById(projectId);
        Optional<FunctionPointAnalysis> optionalAnalysis = functionPointAnalysisService.findByProjectId(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToFunctionPointAdd(projectId);
        }

        boolean updated = functionPointAnalysisService
                .updateSystemBoundaryDescription(projectId, systemBoundaryDescription);

        if (!updated) {
            model.addAttribute("project", optionalProject.get());
            model.addAttribute("analysis", optionalAnalysis.get());
            model.addAttribute("systemBoundaryDescription", systemBoundaryDescription);
            model.addAttribute("error", "El límite del sistema no puede estar vacío.");
            return "fp/edit";
        }

        return redirectToFunctionPointDetails(projectId);
    }

    @GetMapping("/function-points/requirements/update")
    public String updateRequirementsSection(@PathVariable Long projectId,
                                            @RequestParam(name = "requirementsPage", defaultValue = "0") int requirementsPage,
                                            @RequestParam(name = "dataFunctionsPage", defaultValue = "0") int dataFunctionsPage,
                                            @RequestParam(name = "transactionalFunctionsPage", defaultValue = "0") int transactionalFunctionsPage,
                                            Model model) {
        Optional<EstimationProject> optionalProject = estimationProjectService.findById(projectId);
        Optional<FunctionPointAnalysis> optionalAnalysis = functionPointAnalysisService.findDetailedByProjectId(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToFunctionPointAdd(projectId);
        }

        Page<UserRequirement> requirementsPageResult =
                userRequirementService.findPageByProjectId(projectId, PageRequest.of(requirementsPage, 5));

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("requirementsList", requirementsPageResult.getContent());
        model.addAttribute("requirementsPage", requirementsPageResult);
        model.addAttribute("dataFunctionsCurrentPage", dataFunctionsPage);
        model.addAttribute("transactionalFunctionsCurrentPage", transactionalFunctionsPage);

        return "fp/details :: requirementsSection";
    }

    @GetMapping("/function-points/data-functions/update")
    public String updateDataFunctionsSection(@PathVariable Long projectId,
                                             @RequestParam(name = "requirementsPage", defaultValue = "0") int requirementsPage,
                                             @RequestParam(name = "dataFunctionsPage", defaultValue = "0") int dataFunctionsPage,
                                             @RequestParam(name = "transactionalFunctionsPage", defaultValue = "0") int transactionalFunctionsPage,
                                             Model model) {
        Optional<EstimationProject> optionalProject = estimationProjectService.findById(projectId);
        Optional<FunctionPointAnalysis> optionalAnalysis = functionPointAnalysisService.findDetailedByProjectId(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToFunctionPointAdd(projectId);
        }

        Page<DataFunction> dataFunctionsPageResult =
                functionPointAnalysisService.findDataFunctionsPageByProjectId(projectId, PageRequest.of(dataFunctionsPage, 5));

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("dataFunctionsList", dataFunctionsPageResult.getContent());
        model.addAttribute("dataFunctionsPage", dataFunctionsPageResult);
        model.addAttribute("requirementsCurrentPage", requirementsPage);
        model.addAttribute("transactionalFunctionsCurrentPage", transactionalFunctionsPage);

        return "fp/details :: dataFunctionsSection";
    }

    @GetMapping("/function-points/transactional-functions/update")
    public String updateTransactionalFunctionsSection(@PathVariable Long projectId,
                                                      @RequestParam(name = "requirementsPage", defaultValue = "0") int requirementsPage,
                                                      @RequestParam(name = "dataFunctionsPage", defaultValue = "0") int dataFunctionsPage,
                                                      @RequestParam(name = "transactionalFunctionsPage", defaultValue = "0") int transactionalFunctionsPage,
                                                      Model model) {
        Optional<EstimationProject> optionalProject = estimationProjectService.findById(projectId);
        Optional<FunctionPointAnalysis> optionalAnalysis = functionPointAnalysisService.findDetailedByProjectId(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToFunctionPointAdd(projectId);
        }

        Page<TransactionalFunction> transactionalFunctionsPageResult =
                functionPointAnalysisService.findTransactionalFunctionsPageByProjectId(projectId, PageRequest.of(transactionalFunctionsPage, 5));

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("transactionalFunctionsList", transactionalFunctionsPageResult.getContent());
        model.addAttribute("transactionalFunctionsPage", transactionalFunctionsPageResult);
        model.addAttribute("requirementsCurrentPage", requirementsPage);
        model.addAttribute("dataFunctionsCurrentPage", dataFunctionsPage);

        return "fp/details :: transactionalFunctionsSection";
    }

    @GetMapping("/function-points/gsc/edit")
    public String getEditGscForm(@PathVariable Long projectId, Model model) {
        Optional<EstimationProject> optionalProject = estimationProjectService.findById(projectId);
        Optional<FunctionPointAnalysis> optionalAnalysis = functionPointAnalysisService.findDetailedByProjectId(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (optionalAnalysis.isEmpty()) {
            return redirectToFunctionPointAdd(projectId);
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("analysis", optionalAnalysis.get());

        return "fp/gsc-edit";
    }

    @PostMapping("/function-points/gsc/edit")
    public String updateGsc(@PathVariable Long projectId,
                            @ModelAttribute("analysis") FunctionPointAnalysis formAnalysis) {
        boolean updated = functionPointAnalysisService.updateGeneralSystemCharacteristics(projectId, formAnalysis);

        if (!updated) {
            return redirectToFunctionPointAdd(projectId);
        }

        return redirectToFunctionPointDetails(projectId);
    }

    @GetMapping("/requirements/{requirementId}/data-functions/edit/{dataFunctionId}")
    public String getEditDataFunctionFromRequirementForm(@PathVariable Long projectId,
                                                         @PathVariable Long requirementId,
                                                         @PathVariable Long dataFunctionId,
                                                         Model model) {
        Optional<EstimationProject> optionalProject = estimationProjectService.findById(projectId);
        Optional<DataFunction> optionalDataFunction = functionPointAnalysisService.findDataFunction(projectId, dataFunctionId);

        if (optionalProject.isEmpty() || optionalDataFunction.isEmpty()) {
            return redirectToFunctionPointDetails(projectId);
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("requirementId", requirementId);
        model.addAttribute("dataFunction", optionalDataFunction.get());
        model.addAttribute("dataFunctionTypes", DataFunctionType.values());

        return "fp/data-function-edit";
    }

    @PostMapping("/requirements/{requirementId}/data-functions/edit/{dataFunctionId}")
    public String updateDataFunctionFromRequirement(@PathVariable Long projectId,
                                                    @PathVariable Long requirementId,
                                                    @PathVariable Long dataFunctionId,
                                                    @ModelAttribute DataFunction formDataFunction) {
        functionPointAnalysisService.updateDataFunction(projectId, dataFunctionId, formDataFunction);
        return redirectToRequirementDetails(projectId, requirementId);
    }

    @GetMapping("/requirements/{requirementId}/transactional-functions/edit/{transactionalFunctionId}")
    public String getEditTransactionalFunctionFromRequirementForm(@PathVariable Long projectId,
                                                                  @PathVariable Long requirementId,
                                                                  @PathVariable Long transactionalFunctionId,
                                                                  Model model) {
        Optional<EstimationProject> optionalProject = estimationProjectService.findById(projectId);
        Optional<TransactionalFunction> optionalTransactionalFunction =
                functionPointAnalysisService.findTransactionalFunction(projectId, transactionalFunctionId);

        if (optionalProject.isEmpty() || optionalTransactionalFunction.isEmpty()) {
            return redirectToFunctionPointDetails(projectId);
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("requirementId", requirementId);
        model.addAttribute("transactionalFunction", optionalTransactionalFunction.get());
        model.addAttribute("transactionalFunctionTypes", TransactionalFunctionType.values());

        return "fp/transactional-function-edit";
    }

    @PostMapping("/requirements/{requirementId}/transactional-functions/edit/{transactionalFunctionId}")
    public String updateTransactionalFunctionFromRequirement(@PathVariable Long projectId,
                                                             @PathVariable Long requirementId,
                                                             @PathVariable Long transactionalFunctionId,
                                                             @ModelAttribute TransactionalFunction formTransactionalFunction) {
        functionPointAnalysisService.updateTransactionalFunction(projectId, transactionalFunctionId, formTransactionalFunction);
        return redirectToRequirementDetails(projectId, requirementId);
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

    private String redirectToRequirementDetails(Long projectId, Long requirementId) {
        return "redirect:/projects/" + projectId + "/requirements/" + requirementId;
    }
}