package com.uniovi.estimacion.validators.sizeanalyses.functionpoints;

import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.modules.EstimationModule;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

@Component
public class EstimationModuleValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return EstimationModule.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        EstimationModule module = (EstimationModule) target;

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "Error.empty");

        if (module.getName() != null) {
            String name = module.getName().trim();

            if (name.isEmpty() || name.length() > 100) {
                errors.rejectValue("name", "Error.module.name.length");
            }
        }

        if (module.getDescription() != null) {
            String description = module.getDescription().trim();

            if (description.length() > 1000) {
                errors.rejectValue("description", "Error.module.description.length");
            }
        }
    }
}