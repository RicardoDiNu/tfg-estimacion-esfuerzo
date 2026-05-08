package com.uniovi.estimacion.validators.usecasepoints;

import com.uniovi.estimacion.web.forms.usecasepoints.UseCasePointAnalysisForm;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class UseCasePointAnalysisValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return UseCasePointAnalysisForm.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        UseCasePointAnalysisForm form = (UseCasePointAnalysisForm) target;

        if (form.getSystemBoundaryDescription() == null
                || form.getSystemBoundaryDescription().trim().isEmpty()) {
            errors.rejectValue(
                    "systemBoundaryDescription",
                    "ucp.validation.systemBoundary.empty"
            );
        } else if (form.getSystemBoundaryDescription().trim().length() > 2000) {
            errors.rejectValue(
                    "systemBoundaryDescription",
                    "ucp.validation.systemBoundary.tooLong"
            );
        }
    }
}