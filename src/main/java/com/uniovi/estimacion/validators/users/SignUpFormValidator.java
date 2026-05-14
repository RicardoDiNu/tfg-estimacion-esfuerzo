package com.uniovi.estimacion.validators.users;

import com.uniovi.estimacion.services.users.UserService;
import com.uniovi.estimacion.web.forms.users.SignUpForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
@RequiredArgsConstructor
public class SignUpFormValidator implements Validator {

    private final UserService userService;

    @Override
    public boolean supports(Class<?> clazz) {
        return SignUpForm.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        SignUpForm form = (SignUpForm) target;

        String username = normalize(form.getUsername());
        String email = normalize(form.getEmail());
        String password = form.getPassword();
        String passwordConfirm = form.getPasswordConfirm();

        if (!StringUtils.hasText(username)) {
            errors.rejectValue("username", "signup.username.empty", "Debes indicar un nombre de usuario.");
        } else {
            if (username.length() < 3 || username.length() > 50) {
                errors.rejectValue("username", "signup.username.length",
                        "El nombre de usuario debe tener entre 3 y 50 caracteres.");
            }
            if (userService.existsByUsername(username)) {
                errors.rejectValue("username", "signup.username.duplicate",
                        "Ese nombre de usuario ya está en uso.");
            }
        }

        if (!StringUtils.hasText(email)) {
            errors.rejectValue("email", "signup.email.empty", "Debes indicar un email.");
        } else {
            if (email.length() > 100) {
                errors.rejectValue("email", "signup.email.length",
                        "El email no puede superar los 100 caracteres.");
            }
            if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                errors.rejectValue("email", "signup.email.invalid", "Debes indicar un email válido.");
            }
            if (userService.existsByEmail(email)) {
                errors.rejectValue("email", "signup.email.duplicate",
                        "Ese email ya está en uso.");
            }
        }

        if (!StringUtils.hasText(password)) {
            errors.rejectValue("password", "signup.password.empty", "Debes indicar una contraseña.");
        } else if (password.length() < 6 || password.length() > 100) {
            errors.rejectValue("password", "signup.password.length",
                    "La contraseña debe tener entre 6 y 100 caracteres.");
        }

        if (!StringUtils.hasText(passwordConfirm)) {
            errors.rejectValue("passwordConfirm", "signup.passwordConfirm.empty",
                    "Debes repetir la contraseña.");
        } else if (StringUtils.hasText(password) && !password.equals(passwordConfirm)) {
            errors.rejectValue("passwordConfirm", "signup.passwordConfirm.mismatch",
                    "Las contraseñas no coinciden.");
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}