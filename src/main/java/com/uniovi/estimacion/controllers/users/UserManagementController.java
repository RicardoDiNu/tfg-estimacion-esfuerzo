package com.uniovi.estimacion.controllers.users;

import com.uniovi.estimacion.entities.users.User;
import com.uniovi.estimacion.entities.users.UserRole;
import com.uniovi.estimacion.services.users.UserManagementService;
import com.uniovi.estimacion.validators.users.UserManagementFormValidator;
import com.uniovi.estimacion.web.forms.users.UserManagementForm;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserManagementService userManagementService;
    private final UserManagementFormValidator userManagementFormValidator;

    @GetMapping
    public String listUsers(@RequestParam(name = "page", defaultValue = "0") int page,
                            Model model) {
        if (!userManagementService.canManageUsers()) {
            return redirectToProjects();
        }

        addUsersListAttributes(model, page);

        return "users/list";
    }

    @GetMapping("/update")
    public String updateUsersSection(@RequestParam(name = "page", defaultValue = "0") int page,
                                     Model model) {
        if (!userManagementService.canManageUsers()) {
            return redirectToProjects();
        }

        addUsersListAttributes(model, page);

        return "users/list :: usersSection";
    }

    @GetMapping("/add")
    public String getAddForm(Model model) {
        if (!userManagementService.canManageUsers()) {
            return redirectToProjects();
        }

        UserManagementForm form = new UserManagementForm();

        if (userManagementService.findCreatableRolesForCurrentUser().contains(UserRole.ROLE_PROJECT_WORKER)
                && userManagementService.findCreatableRolesForCurrentUser().size() == 1) {
            form.setRole(UserRole.ROLE_PROJECT_WORKER);
        }

        addFormAttributes(model, form);

        return "users/add";
    }

    @PostMapping("/add")
    public String addUser(@ModelAttribute("userForm") UserManagementForm form,
                          BindingResult result,
                          Model model) {
        if (!userManagementService.canManageUsers()) {
            return redirectToProjects();
        }

        userManagementFormValidator.validateCreate(form, result);

        if (result.hasErrors()) {
            addFormAttributes(model, form);
            return "users/add";
        }

        Optional<User> createdUser =
                userManagementService.createUserForCurrentUser(
                        form.getUsername(),
                        form.getEmail(),
                        form.getPassword(),
                        form.getRole(),
                        form.getProjectManagerId()
                );

        if (createdUser.isEmpty()) {
            result.reject("user.management.validation.create.invalid");
            addFormAttributes(model, form);
            return "users/add";
        }

        return redirectToUsers();
    }

    @GetMapping("/{userId}/edit")
    public String getEditForm(@PathVariable Long userId,
                              Model model) {
        Optional<User> optionalUser =
                userManagementService.findManageableUserByIdForCurrentUser(userId);

        if (optionalUser.isEmpty()) {
            return redirectToUsers();
        }

        User user = optionalUser.get();
        UserManagementForm form = buildForm(user);

        addEditAttributes(model, form, user);

        return "users/edit";
    }

    private void addEditAttributes(Model model, UserManagementForm form, User editedUser) {
        model.addAttribute("userForm", form);
        model.addAttribute("editedUser", editedUser);
    }

    @PostMapping("/{userId}/edit")
    public String editUser(@PathVariable Long userId,
                           @ModelAttribute("userForm") UserManagementForm form,
                           BindingResult result,
                           Model model) {
        Optional<User> optionalUser =
                userManagementService.findManageableUserByIdForCurrentUser(userId);

        if (optionalUser.isEmpty()) {
            return redirectToUsers();
        }

        User editedUser = optionalUser.get();

        form.setId(userId);
        form.setRole(editedUser.getRole());

        if (editedUser.getProjectManager() != null) {
            form.setProjectManagerId(editedUser.getProjectManager().getId());
        }

        userManagementFormValidator.validateUpdate(form, result);

        if (result.hasErrors()) {
            addEditAttributes(model, form, editedUser);
            return "users/edit";
        }

        boolean updated =
                userManagementService.updateUserForCurrentUser(
                        userId,
                        form.getUsername(),
                        form.getEmail(),
                        form.getPassword()
                );

        if (!updated) {
            result.reject("user.management.validation.update.invalid");
            addEditAttributes(model, form, editedUser);
            return "users/edit";
        }

        return redirectToUsers();
    }

    @GetMapping("/{userId}/delete")
    public String deleteUser(@PathVariable Long userId) {
        userManagementService.deleteUserForCurrentUser(userId);
        return redirectToUsers();
    }

    private void addUsersListAttributes(Model model, int page) {
        Page<User> usersPage =
                userManagementService.findPageVisibleForCurrentUser(PageRequest.of(Math.max(page, 0), 5));

        model.addAttribute("usersList", usersPage.getContent());
        model.addAttribute("usersPage", usersPage);
    }

    private void addFormAttributes(Model model, UserManagementForm form) {
        model.addAttribute("userForm", form);
        model.addAttribute("availableRoles", userManagementService.findCreatableRolesForCurrentUser());
        model.addAttribute("availableProjectManagers", userManagementService.findAvailableProjectManagersForAdmin());
    }

    private UserManagementForm buildForm(User user) {
        UserManagementForm form = new UserManagementForm();

        form.setId(user.getId());
        form.setUsername(user.getUsername());
        form.setEmail(user.getEmail());
        form.setRole(user.getRole());
        form.setEnabled(user.getEnabled());

        if (user.getProjectManager() != null) {
            form.setProjectManagerId(user.getProjectManager().getId());
        }

        return form;
    }

    private String redirectToUsers() {
        return "redirect:/users";
    }

    private String redirectToProjects() {
        return "redirect:/projects";
    }
}