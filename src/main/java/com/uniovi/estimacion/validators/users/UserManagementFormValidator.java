package com.uniovi.estimacion.validators.users;

import com.uniovi.estimacion.entities.users.UserRole;
import com.uniovi.estimacion.services.users.UserManagementService;
import com.uniovi.estimacion.web.forms.users.UserManagementForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;

@Component
@RequiredArgsConstructor
public class UserManagementFormValidator {

    private final UserManagementService userManagementService;

    public void validateCreate(UserManagementForm form, Errors errors) {
        validateCommonFields(form, errors, true);
        validateRoleForCreate(form, errors);
    }

    public void validateUpdate(UserManagementForm form, Errors errors) {
        validateCommonFields(form, errors, false);
    }

    private void validateCommonFields(UserManagementForm form, Errors errors, boolean passwordRequired) {
        if (!StringUtils.hasText(form.getUsername())) {
            errors.rejectValue("username", "user.management.validation.username.empty");
        } else {
            String username = form.getUsername().trim();

            if (form.getId() == null) {
                if (userManagementService.existsByUsername(username)) {
                    errors.rejectValue("username", "user.management.validation.username.duplicate");
                }
            } else {
                if (userManagementService.existsByUsernameExcludingId(username, form.getId())) {
                    errors.rejectValue("username", "user.management.validation.username.duplicate");
                }
            }
        }

        if (!StringUtils.hasText(form.getEmail())) {
            errors.rejectValue("email", "user.management.validation.email.empty");
        } else {
            String email = form.getEmail().trim();

            if (form.getId() == null) {
                if (userManagementService.existsByEmail(email)) {
                    errors.rejectValue("email", "user.management.validation.email.duplicate");
                }
            } else {
                if (userManagementService.existsByEmailExcludingId(email, form.getId())) {
                    errors.rejectValue("email", "user.management.validation.email.duplicate");
                }
            }
        }

        boolean passwordHasText = StringUtils.hasText(form.getPassword());
        boolean passwordConfirmHasText = StringUtils.hasText(form.getPasswordConfirm());

        if (passwordRequired && !passwordHasText) {
            errors.rejectValue("password", "user.management.validation.password.empty");
        }

        if (passwordRequired && !passwordConfirmHasText) {
            errors.rejectValue("passwordConfirm", "user.management.validation.passwordConfirm.empty");
        }

        if ((passwordHasText || passwordConfirmHasText)
                && !String.valueOf(form.getPassword()).equals(String.valueOf(form.getPasswordConfirm()))) {
            errors.rejectValue("passwordConfirm", "user.management.validation.passwordConfirm.mismatch");
        }
    }

    private void validateRoleForCreate(UserManagementForm form, Errors errors) {
        if (form.getRole() == null) {
            errors.rejectValue("role", "user.management.validation.role.empty");
            return;
        }

        if (!userManagementService.findCreatableRolesForCurrentUser().contains(form.getRole())) {
            errors.rejectValue("role", "user.management.validation.role.invalid");
            return;
        }

        validateProjectManagerIfWorker(form, errors);
    }


    private void validateProjectManagerIfWorker(UserManagementForm form, Errors errors) {
        if (form.getRole() == UserRole.ROLE_PROJECT_WORKER
                && userManagementService.findAvailableProjectManagersForAdmin().size() > 0
                && form.getProjectManagerId() == null) {
            errors.rejectValue("projectManagerId", "user.management.validation.projectManager.empty");
        }
    }
}