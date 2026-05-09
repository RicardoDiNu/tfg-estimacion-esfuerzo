package com.uniovi.estimacion.validators.sizeanalyses.usecasepoints;

import com.uniovi.estimacion.web.forms.sizeanalyses.usecasepoints.UseCaseActorForm;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class UseCaseActorValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return UseCaseActorForm.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        UseCaseActorForm form = (UseCaseActorForm) target;

        if (form.getName() == null || form.getName().trim().isEmpty()) {
            errors.rejectValue("name", "ucp.actor.validation.name.empty");
        } else if (form.getName().trim().length() > 150) {
            errors.rejectValue("name", "ucp.actor.validation.name.tooLong");
        }

        if (form.getDescription() != null && form.getDescription().length() > 1000) {
            errors.rejectValue("description", "ucp.actor.validation.description.tooLong");
        }

        if (form.getComplexity() == null) {
            errors.rejectValue("complexity", "ucp.actor.validation.complexity.empty");
        }
    }
}