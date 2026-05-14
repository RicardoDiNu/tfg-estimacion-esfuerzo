package com.uniovi.estimacion.validators.effortconversions.transformationfunctions;

import com.uniovi.estimacion.web.forms.effortconversions.transformationfunctions.TransformationFunctionForm;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class TransformationFunctionValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return TransformationFunctionForm.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        TransformationFunctionForm form = (TransformationFunctionForm) target;

        if (form.getName() == null || form.getName().trim().isEmpty()) {
            errors.rejectValue("name", "transformationFunction.validation.name.empty");
        } else if (form.getName().trim().length() > 120) {
            errors.rejectValue("name", "transformationFunction.validation.name.tooLong");
        }

        if (form.getDescription() != null && form.getDescription().length() > 1000) {
            errors.rejectValue("description", "transformationFunction.validation.description.tooLong");
        }

        if (form.getIntercept() == null) {
            errors.rejectValue("intercept", "transformationFunction.validation.intercept.empty");
        } else if (form.getIntercept() < 0) {
            errors.rejectValue("intercept", "transformationFunction.validation.intercept.negative");
        }

        if (form.getSlope() == null) {
            errors.rejectValue("slope", "transformationFunction.validation.slope.empty");
        } else if (form.getSlope() <= 0) {
            errors.rejectValue("slope", "transformationFunction.validation.slope.notPositive");
        }
    }
}