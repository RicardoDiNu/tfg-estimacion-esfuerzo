package com.uniovi.estimacion.controllers.projects;

import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.projects.ProjectMembership;
import com.uniovi.estimacion.entities.users.User;
import com.uniovi.estimacion.services.projects.EstimationProjectService;
import com.uniovi.estimacion.services.projects.ProjectMembershipService;
import com.uniovi.estimacion.web.forms.projects.ProjectMembershipForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/projects/{projectId}/memberships")
@RequiredArgsConstructor
public class ProjectMembershipController {

    private final EstimationProjectService estimationProjectService;
    private final ProjectMembershipService projectMembershipService;

    @GetMapping
    public String getMemberships(@PathVariable Long projectId,
                                 Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findManageableByIdForCurrentUser(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        addCommonModelAttributes(model, optionalProject.get(), new ProjectMembershipForm());

        return "project/memberships";
    }

    @PostMapping("/assign")
    public String assignWorker(@PathVariable Long projectId,
                               @ModelAttribute("membershipForm") ProjectMembershipForm membershipForm,
                               BindingResult result,
                               Model model) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findManageableByIdForCurrentUser(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        if (membershipForm.getWorkerId() == null) {
            result.rejectValue("workerId", "project.membership.validation.worker.empty");
        }

        if (result.hasErrors()) {
            addCommonModelAttributes(model, optionalProject.get(), membershipForm);
            return "project/memberships";
        }

        Optional<ProjectMembership> savedMembership =
                projectMembershipService.assignOrUpdateWorker(
                        projectId,
                        membershipForm.getWorkerId(),
                        membershipForm.getCanEditEstimationData(),
                        membershipForm.getCanManageEffortConversions()
                );

        if (savedMembership.isEmpty()) {
            result.rejectValue("workerId", "project.membership.validation.worker.invalid");
            addCommonModelAttributes(model, optionalProject.get(), membershipForm);
            return "project/memberships";
        }

        return redirectToMemberships(projectId);
    }

    @PostMapping("/{workerId}/remove")
    public String removeWorker(@PathVariable Long projectId,
                               @PathVariable Long workerId) {
        Optional<EstimationProject> optionalProject =
                estimationProjectService.findManageableByIdForCurrentUser(projectId);

        if (optionalProject.isEmpty()) {
            return redirectToProjects();
        }

        projectMembershipService.removeWorkerFromProject(projectId, workerId);

        return redirectToMemberships(projectId);
    }

    private void addCommonModelAttributes(Model model,
                                          EstimationProject project,
                                          ProjectMembershipForm membershipForm) {
        List<ProjectMembership> memberships =
                projectMembershipService.findByProjectId(project.getId());

        List<User> availableWorkers =
                projectMembershipService.findAssignableWorkers();

        model.addAttribute("project", project);
        model.addAttribute("memberships", memberships);
        model.addAttribute("availableWorkers", availableWorkers);
        model.addAttribute("membershipForm", membershipForm);
    }

    private String redirectToProjects() {
        return "redirect:/projects";
    }

    private String redirectToMemberships(Long projectId) {
        return "redirect:/projects/" + projectId + "/memberships";
    }
}