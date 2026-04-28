package com.uniovi.estimacion.validators.functionpoints;

import com.uniovi.estimacion.entities.functionpoints.FunctionPointAnalysis;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

@Component
public class FunctionPointAnalysisValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return FunctionPointAnalysis.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        FunctionPointAnalysis analysis = (FunctionPointAnalysis) target;

        ValidationUtils.rejectIfEmptyOrWhitespace(
                errors,
                "systemBoundaryDescription",
                "Error.empty"
        );

        if (analysis.getSystemBoundaryDescription() != null) {
            String boundary = analysis.getSystemBoundaryDescription().trim();

            if (boundary.isEmpty() || boundary.length() > 2000) {
                errors.rejectValue(
                        "systemBoundaryDescription",
                        "Error.functionPointAnalysis.systemBoundaryDescription.length"
                );
            }
        }
    }
}