package com.uniovi.estimacion.validators.projects;

import com.uniovi.estimacion.entities.projects.EstimationProject;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import java.math.BigDecimal;

@Component
public class EstimationProjectValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return EstimationProject.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        EstimationProject project = (EstimationProject) target;

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "Error.empty");

        if (project.getName() != null) {
            String name = project.getName().trim();

            if (name.isEmpty() || name.length() > 100) {
                errors.rejectValue("name", "Error.project.name.length");
            }
        }

        if (project.getDescription() != null) {
            String description = project.getDescription().trim();

            if (description.length() > 1000) {
                errors.rejectValue("description", "Error.project.description.length");
            }
        }

        if (project.getHourlyRate() != null
                && project.getHourlyRate().compareTo(BigDecimal.ZERO) < 0) {
            errors.rejectValue("hourlyRate", "project.validation.hourlyRate.negative");
        }

        if (project.getCurrencyCode() != null
                && !project.getCurrencyCode().trim().isEmpty()
                && project.getCurrencyCode().trim().length() != 3) {
            errors.rejectValue("currencyCode", "project.validation.currencyCode.invalid");
        }
    }
}