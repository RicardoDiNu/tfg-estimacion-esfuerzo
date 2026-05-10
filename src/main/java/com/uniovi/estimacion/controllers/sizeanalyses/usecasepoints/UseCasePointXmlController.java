package com.uniovi.estimacion.controllers.sizeanalyses.usecasepoints;

import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.services.projects.EstimationProjectService;
import com.uniovi.estimacion.services.projects.ProjectAuthorizationService;
import com.uniovi.estimacion.services.sizeanalyses.usecasepoints.InvalidUseCasePointXmlException;
import com.uniovi.estimacion.services.sizeanalyses.usecasepoints.UseCasePointAnalysisService;
import com.uniovi.estimacion.services.sizeanalyses.usecasepoints.UseCasePointXmlExportService;
import com.uniovi.estimacion.services.sizeanalyses.usecasepoints.UseCasePointXmlImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.Optional;

@Controller
@RequestMapping("/projects/{projectId}")
@RequiredArgsConstructor
public class UseCasePointXmlController {

    private final EstimationProjectService estimationProjectService;
    private final UseCasePointAnalysisService useCasePointAnalysisService;
    private final UseCasePointXmlExportService useCasePointXmlExportService;
    private final UseCasePointXmlImportService useCasePointXmlImportService;
    private final ProjectAuthorizationService projectAuthorizationService;

    @GetMapping("/use-case-points/export/xml")
    public ResponseEntity<byte[]> exportXml(@PathVariable Long projectId) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);

        if (optionalProject.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (useCasePointAnalysisService.findByProjectId(projectId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Optional<byte[]> xmlBytes = useCasePointXmlExportService.exportToXml(projectId);

        if (xmlBytes.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("analisis-ucp-proyecto-" + projectId + ".xml")
                        .build()
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(xmlBytes.get());
    }

    @GetMapping("/use-case-points/import")
    public String getImportForm(@PathVariable Long projectId, Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findAccessibleByIdForCurrentUser(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (!projectAuthorizationService.canManageProject(projectId)) {
            return redirectToProjects();
        }

        if (useCasePointAnalysisService.findByProjectId(projectId).isPresent()) {
            return redirectToUseCasePointDetails(projectId);
        }

        model.addAttribute("project", optionalProject.get());
        return "ucp/import";
    }

    @PostMapping("/use-case-points/import")
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

        if (useCasePointAnalysisService.findByProjectId(projectId).isPresent()) {
            redirectAttributes.addFlashAttribute("ucpErrorKey", "ucp.import.error.existingAnalysis");
            return "redirect:/projects/" + projectId;
        }

        EstimationProject project = optionalProject.get();

        if (file == null || file.isEmpty()) {
            model.addAttribute("project", project);
            model.addAttribute("errorKey", "ucp.import.error.invalid");
            return "ucp/import";
        }

        try {
            byte[] xmlBytes = file.getBytes();
            useCasePointXmlImportService.importFromXml(project, xmlBytes);
            redirectAttributes.addFlashAttribute("ucpImportSuccess", true);
            return redirectToUseCasePointDetails(projectId);
        } catch (InvalidUseCasePointXmlException e) {
            model.addAttribute("project", project);
            model.addAttribute("errorKey", "ucp.import.error.invalid");
            return "ucp/import";
        } catch (IOException e) {
            model.addAttribute("project", project);
            model.addAttribute("errorKey", "ucp.import.error.invalid");
            return "ucp/import";
        }
    }

    private String redirectToProjects() {
        return "redirect:/projects";
    }

    private String redirectToUseCasePointDetails(Long projectId) {
        return "redirect:/projects/" + projectId + "/use-case-points";
    }
}
