package com.uniovi.estimacion.controllers;

import com.uniovi.estimacion.entities.EstimationProject;
import com.uniovi.estimacion.entities.functionpoints.DataFunction;
import com.uniovi.estimacion.entities.functionpoints.DataFunctionType;
import com.uniovi.estimacion.entities.functionpoints.FunctionPointAnalysis;
import com.uniovi.estimacion.entities.functionpoints.TransactionalFunction;
import com.uniovi.estimacion.entities.functionpoints.TransactionalFunctionType;
import com.uniovi.estimacion.services.EstimationProjectService;
import com.uniovi.estimacion.services.functionpoints.FunctionPointAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

import com.uniovi.estimacion.services.requirements.UserRequirementService;

@Controller
@RequiredArgsConstructor
public class FunctionPointAnalysisController {

    private final EstimationProjectService estimationProjectService;
    private final FunctionPointAnalysisService functionPointAnalysisService;
    private final UserRequirementService userRequirementService;

    @GetMapping("/projects/{projectId}/function-points/add")
    public String getCreateForm(@PathVariable Long projectId, Model model) {
        Optional<EstimationProject> optionalProject = estimationProjectService.getProject(projectId);

        if (optionalProject.isEmpty()) {
            return "redirect:/projects";
        }

        if (functionPointAnalysisService.getByProjectId(projectId).isPresent()) {
            return "redirect:/projects/" + projectId + "/function-points";
        }

        model.addAttribute("project", optionalProject.get());
        return "fp/add";
    }

    @PostMapping("/projects/{projectId}/function-points/add")
    public String createAnalysis(@PathVariable Long projectId,
                                 @RequestParam("systemBoundaryDescription") String systemBoundaryDescription) {
        Optional<EstimationProject> optionalProject = estimationProjectService.getProject(projectId);

        if (optionalProject.isEmpty()) {
            return "redirect:/projects";
        }

        if (functionPointAnalysisService.getByProjectId(projectId).isPresent()) {
            return "redirect:/projects/" + projectId + "/function-points";
        }

        functionPointAnalysisService.createInitialAnalysis(optionalProject.get(), systemBoundaryDescription);
        return "redirect:/projects/" + projectId + "/function-points";
    }

    @GetMapping("/projects/{projectId}/function-points")
    public String getFunctionPointAnalysisDetails(@PathVariable Long projectId, Model model) {
        Optional<EstimationProject> optionalProject = estimationProjectService.getProject(projectId);
        Optional<FunctionPointAnalysis> optionalAnalysis = functionPointAnalysisService.getDetailedByProjectId(projectId);

        if (optionalProject.isEmpty()) {
            return "redirect:/projects";
        }

        if (optionalAnalysis.isEmpty()) {
            return "redirect:/projects/" + projectId + "/function-points/add";
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("analysis", optionalAnalysis.get());
        model.addAttribute("requirements", userRequirementService.getByProjectId(projectId));

        return "fp/details";
    }

    @GetMapping("/projects/{projectId}/function-points/data-functions/add")
    public String getAddDataFunctionForm(@PathVariable Long projectId, Model model) {
        Optional<FunctionPointAnalysis> optionalAnalysis = functionPointAnalysisService.getByProjectId(projectId);

        if (optionalAnalysis.isEmpty()) {
            return "redirect:/projects/" + projectId + "/function-points/add";
        }

        model.addAttribute("analysis", optionalAnalysis.get());
        model.addAttribute("dataFunction", new DataFunction());
        model.addAttribute("dataFunctionTypes", DataFunctionType.values());

        return "fp/data-function-add";
    }

    @PostMapping("/projects/{projectId}/function-points/data-functions/add")
    public String addDataFunction(@PathVariable Long projectId,
                                  @ModelAttribute DataFunction dataFunction) {
        boolean added = functionPointAnalysisService.addDataFunctionToProject(projectId, dataFunction);

        if (!added) {
            return "redirect:/projects/" + projectId + "/function-points/add";
        }

        return "redirect:/projects/" + projectId + "/function-points";
    }

    @GetMapping("/projects/{projectId}/function-points/transactional-functions/add")
    public String getAddTransactionalFunctionForm(@PathVariable Long projectId, Model model) {
        Optional<FunctionPointAnalysis> optionalAnalysis = functionPointAnalysisService.getByProjectId(projectId);

        if (optionalAnalysis.isEmpty()) {
            return "redirect:/projects/" + projectId + "/function-points/add";
        }

        model.addAttribute("analysis", optionalAnalysis.get());
        model.addAttribute("transactionalFunction", new TransactionalFunction());
        model.addAttribute("transactionalFunctionTypes", TransactionalFunctionType.values());

        return "fp/transactional-function-add";
    }

    @PostMapping("/projects/{projectId}/function-points/transactional-functions/add")
    public String addTransactionalFunction(@PathVariable Long projectId,
                                           @ModelAttribute TransactionalFunction transactionalFunction) {
        boolean added = functionPointAnalysisService.addTransactionalFunctionToProject(projectId, transactionalFunction);

        if (!added) {
            return "redirect:/projects/" + projectId + "/function-points/add";
        }

        return "redirect:/projects/" + projectId + "/function-points";
    }

    @GetMapping("/projects/{projectId}/function-points/gsc/edit")
    public String getEditGscForm(@PathVariable Long projectId, Model model) {
        Optional<EstimationProject> optionalProject = estimationProjectService.getProject(projectId);
        Optional<FunctionPointAnalysis> optionalAnalysis = functionPointAnalysisService.getDetailedByProjectId(projectId);

        if (optionalProject.isEmpty()) {
            return "redirect:/projects";
        }

        if (optionalAnalysis.isEmpty()) {
            return "redirect:/projects/" + projectId + "/function-points/add";
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("analysis", optionalAnalysis.get());

        return "fp/gsc-edit";
    }

    @PostMapping("/projects/{projectId}/function-points/gsc/edit")
    public String updateGsc(@PathVariable Long projectId,
                            @ModelAttribute("analysis") FunctionPointAnalysis formAnalysis) {
        boolean updated = functionPointAnalysisService.updateGeneralSystemCharacteristics(projectId, formAnalysis);

        if (!updated) {
            return "redirect:/projects/" + projectId + "/function-points/add";
        }

        return "redirect:/projects/" + projectId + "/function-points";
    }

    @GetMapping("/projects/{projectId}/function-points/data-functions/delete/{dataFunctionId}")
    public String deleteDataFunction(@PathVariable Long projectId,
                                     @PathVariable Long dataFunctionId) {
        functionPointAnalysisService.deleteDataFunctionFromProject(projectId, dataFunctionId);
        return "redirect:/projects/" + projectId + "/function-points";
    }

    @GetMapping("/projects/{projectId}/function-points/transactional-functions/delete/{transactionalFunctionId}")
    public String deleteTransactionalFunction(@PathVariable Long projectId,
                                              @PathVariable Long transactionalFunctionId) {
        functionPointAnalysisService.deleteTransactionalFunctionFromProject(projectId, transactionalFunctionId);
        return "redirect:/projects/" + projectId + "/function-points";
    }

    @GetMapping("/projects/{projectId}/function-points/data-functions/edit/{dataFunctionId}")
    public String getEditDataFunctionForm(@PathVariable Long projectId,
                                          @PathVariable Long dataFunctionId,
                                          Model model) {
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisService.getDetailedByProjectId(projectId);

        if (optionalAnalysis.isEmpty()) {
            return "redirect:/projects/" + projectId + "/function-points/add";
        }

        Optional<DataFunction> optionalDataFunction = optionalAnalysis.get().getDataFunctions()
                .stream()
                .filter(df -> df.getId().equals(dataFunctionId))
                .findFirst();

        if (optionalDataFunction.isEmpty()) {
            return "redirect:/projects/" + projectId + "/function-points";
        }

        model.addAttribute("analysis", optionalAnalysis.get());
        model.addAttribute("dataFunction", optionalDataFunction.get());
        model.addAttribute("dataFunctionTypes", DataFunctionType.values());

        return "fp/data-function-edit";
    }

    @PostMapping("/projects/{projectId}/function-points/data-functions/edit/{dataFunctionId}")
    public String updateDataFunction(@PathVariable Long projectId,
                                     @PathVariable Long dataFunctionId,
                                     @ModelAttribute DataFunction dataFunction) {
        dataFunction.setId(dataFunctionId);

        boolean updated = functionPointAnalysisService.updateDataFunctionInProject(projectId, dataFunction);

        if (!updated) {
            return "redirect:/projects/" + projectId + "/function-points";
        }

        return "redirect:/projects/" + projectId + "/function-points";
    }

    @GetMapping("/projects/{projectId}/function-points/transactional-functions/edit/{transactionalFunctionId}")
    public String getEditTransactionalFunctionForm(@PathVariable Long projectId,
                                                   @PathVariable Long transactionalFunctionId,
                                                   Model model) {
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisService.getDetailedByProjectId(projectId);

        if (optionalAnalysis.isEmpty()) {
            return "redirect:/projects/" + projectId + "/function-points/add";
        }

        Optional<TransactionalFunction> optionalTransactionalFunction = optionalAnalysis.get().getTransactionalFunctions()
                .stream()
                .filter(tf -> tf.getId().equals(transactionalFunctionId))
                .findFirst();

        if (optionalTransactionalFunction.isEmpty()) {
            return "redirect:/projects/" + projectId + "/function-points";
        }

        model.addAttribute("analysis", optionalAnalysis.get());
        model.addAttribute("transactionalFunction", optionalTransactionalFunction.get());
        model.addAttribute("transactionalFunctionTypes", TransactionalFunctionType.values());

        return "fp/transactional-function-edit";
    }

    @PostMapping("/projects/{projectId}/function-points/transactional-functions/edit/{transactionalFunctionId}")
    public String updateTransactionalFunction(@PathVariable Long projectId,
                                              @PathVariable Long transactionalFunctionId,
                                              @ModelAttribute TransactionalFunction transactionalFunction) {
        transactionalFunction.setId(transactionalFunctionId);

        boolean updated = functionPointAnalysisService
                .updateTransactionalFunctionInProject(projectId, transactionalFunction);

        if (!updated) {
            return "redirect:/projects/" + projectId + "/function-points";
        }

        return "redirect:/projects/" + projectId + "/function-points";
    }

    @GetMapping("/projects/{projectId}/requirements/{requirementId}/data-functions/edit/{dataFunctionId}")
    public String getEditDataFunctionFromRequirementForm(@PathVariable Long projectId,
                                                         @PathVariable Long requirementId,
                                                         @PathVariable Long dataFunctionId,
                                                         Model model) {
        Optional<EstimationProject> optionalProject = estimationProjectService.getProject(projectId);
        Optional<FunctionPointAnalysis> optionalAnalysis = functionPointAnalysisService.getDetailedByProjectId(projectId);
        Optional<DataFunction> optionalDataFunction =
                functionPointAnalysisService.getDataFunctionInProject(projectId, dataFunctionId);

        if (optionalProject.isEmpty() || optionalAnalysis.isEmpty() || optionalDataFunction.isEmpty()) {
            return "redirect:/projects";
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("analysis", optionalAnalysis.get());
        model.addAttribute("requirementId", requirementId);
        model.addAttribute("dataFunction", optionalDataFunction.get());
        model.addAttribute("dataFunctionTypes", DataFunctionType.values());

        return "fp/data-function-edit";
    }

    @PostMapping("/projects/{projectId}/requirements/{requirementId}/data-functions/edit/{dataFunctionId}")
    public String updateDataFunctionFromRequirement(@PathVariable Long projectId,
                                                    @PathVariable Long requirementId,
                                                    @PathVariable Long dataFunctionId,
                                                    @ModelAttribute DataFunction formDataFunction) {
        functionPointAnalysisService.updateDataFunctionInProject(projectId, dataFunctionId, formDataFunction);
        return "redirect:/projects/" + projectId + "/requirements/" + requirementId;
    }

    @GetMapping("/projects/{projectId}/requirements/{requirementId}/transactional-functions/edit/{transactionalFunctionId}")
    public String getEditTransactionalFunctionFromRequirementForm(@PathVariable Long projectId,
                                                                  @PathVariable Long requirementId,
                                                                  @PathVariable Long transactionalFunctionId,
                                                                  Model model) {
        Optional<EstimationProject> optionalProject = estimationProjectService.getProject(projectId);
        Optional<FunctionPointAnalysis> optionalAnalysis = functionPointAnalysisService.getDetailedByProjectId(projectId);
        Optional<TransactionalFunction> optionalTransactionalFunction =
                functionPointAnalysisService.getTransactionalFunctionInProject(projectId, transactionalFunctionId);

        if (optionalProject.isEmpty() || optionalAnalysis.isEmpty() || optionalTransactionalFunction.isEmpty()) {
            return "redirect:/projects";
        }

        model.addAttribute("project", optionalProject.get());
        model.addAttribute("analysis", optionalAnalysis.get());
        model.addAttribute("requirementId", requirementId);
        model.addAttribute("transactionalFunction", optionalTransactionalFunction.get());
        model.addAttribute("transactionalFunctionTypes", TransactionalFunctionType.values());

        return "fp/transactional-function-edit";
    }

    @PostMapping("/projects/{projectId}/requirements/{requirementId}/transactional-functions/edit/{transactionalFunctionId}")
    public String updateTransactionalFunctionFromRequirement(@PathVariable Long projectId,
                                                             @PathVariable Long requirementId,
                                                             @PathVariable Long transactionalFunctionId,
                                                             @ModelAttribute TransactionalFunction formTransactionalFunction) {
        functionPointAnalysisService.updateTransactionalFunctionInProject(projectId, transactionalFunctionId, formTransactionalFunction);
        return "redirect:/projects/" + projectId + "/requirements/" + requirementId;
    }
}