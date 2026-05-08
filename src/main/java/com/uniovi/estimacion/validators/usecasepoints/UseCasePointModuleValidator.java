package com.uniovi.estimacion.validators.usecasepoints;

import com.uniovi.estimacion.web.forms.usecasepoints.UseCasePointModuleForm;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class UseCasePointModuleValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return UseCasePointModuleForm.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        UseCasePointModuleForm form = (UseCasePointModuleForm) target;

        if (form.getName() == null || form.getName().trim().isEmpty()) {
            errors.rejectValue("name", "ucp.module.validation.name.empty");
        } else if (form.getName().trim().length() > 150) {
            errors.rejectValue("name", "ucp.module.validation.name.tooLong");
        }

        if (form.getDescription() != null && form.getDescription().length() > 1000) {
            errors.rejectValue("description", "ucp.module.validation.description.tooLong");
        }
    }
}