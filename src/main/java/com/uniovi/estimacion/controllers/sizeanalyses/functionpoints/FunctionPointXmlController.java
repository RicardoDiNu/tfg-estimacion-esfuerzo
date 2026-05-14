package com.uniovi.estimacion.controllers.sizeanalyses.functionpoints;

import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.services.projects.EstimationProjectService;
import com.uniovi.estimacion.services.projects.ProjectAuthorizationService;
import com.uniovi.estimacion.services.sizeanalyses.functionpoints.FunctionPointAnalysisService;
import com.uniovi.estimacion.services.sizeanalyses.functionpoints.FunctionPointXmlExportService;
import com.uniovi.estimacion.services.sizeanalyses.functionpoints.FunctionPointXmlImportService;
import com.uniovi.estimacion.services.sizeanalyses.functionpoints.InvalidFunctionPointXmlException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.Optional;

@Controller
@RequestMapping("/projects/{projectId}")
@RequiredArgsConstructor
public class FunctionPointXmlController {

    private final EstimationProjectService estimationProjectService;
    private final FunctionPointAnalysisService functionPointAnalysisService;
    private final FunctionPointXmlExportService functionPointXmlExportService;
    private final FunctionPointXmlImportService functionPointXmlImportService;
    private final ProjectAuthorizationService projectAuthorizationService;

    @GetMapping("/function-points/export/xml")
    public ResponseEntity<byte[]> exportXml(@PathVariable Long projectId) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);

        if (optionalProject.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (functionPointAnalysisService.findByProjectId(projectId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Optional<byte[]> xmlBytes = functionPointXmlExportService.exportToXml(projectId);

        if (xmlBytes.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("analisis-pf-proyecto-" + projectId + ".xml")
                        .build()
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(xmlBytes.get());
    }

    @GetMapping("/function-points/import")
    public String getImportForm(@PathVariable Long projectId, Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (!projectAuthorizationService.canManageProject(projectId)) {
            return redirectToProjects();
        }

        if (functionPointAnalysisService.findByProjectId(projectId).isPresent()) {
            return redirectToFunctionPointDetails(projectId);
        }

        model.addAttribute("project", optionalProject.get());
        return "fp/import";
    }

    @PostMapping("/function-points/import")
    public String importXml(@PathVariable Long projectId,
                            @RequestParam("file") MultipartFile file,
                            RedirectAttributes redirectAttributes,
                            Model model) {

        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (!projectAuthorizationService.canManageProject(projectId)) {
            return redirectToProjects();
        }

        if (functionPointAnalysisService.findByProjectId(projectId).isPresent()) {
            redirectAttributes.addFlashAttribute("fpErrorKey", "fp.import.error.existingAnalysis");
            return "redirect:/projects/" + projectId;
        }

        EstimationProject project = optionalProject.get();

        if (file == null || file.isEmpty()) {
            model.addAttribute("project", project);
            model.addAttribute("errorKey", "fp.import.error.invalid");
            return "fp/import";
        }

        try {
            byte[] xmlBytes = file.getBytes();
            functionPointXmlImportService.importFromXml(project, xmlBytes);
            redirectAttributes.addFlashAttribute("fpImportSuccess", true);
            return redirectToFunctionPointDetails(projectId);
        } catch (InvalidFunctionPointXmlException e) {
            model.addAttribute("project", project);
            model.addAttribute("errorKey", e.getMessage());
            return "fp/import";
        } catch (IOException e) {
            model.addAttribute("project", project);
            model.addAttribute("errorKey", "fp.import.error.invalid");
            return "fp/import";
        }
    }

    private String redirectToProjects() {
        return "redirect:/projects";
    }

    private String redirectToFunctionPointDetails(Long projectId) {
        return "redirect:/projects/" + projectId + "/function-points";
    }
}
