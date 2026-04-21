package com.uniovi.estimacion.validators.functionpoints;

import com.uniovi.estimacion.entities.functionpoints.DataFunction;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

@Component
public class DataFunctionValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return DataFunction.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        DataFunction dataFunction = (DataFunction) target;

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "type", "Error.empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "complexity", "Error.empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "Error.empty");

        if (dataFunction.getName() != null) {
            String name = dataFunction.getName().trim();

            if (name.length() < 2 || name.length() > 100) {
                errors.rejectValue("name", "Error.dataFunction.name.length");
            }
        }

        if (dataFunction.getDescription() != null) {
            String description = dataFunction.getDescription().trim();

            if (description.length() > 1000) {
                errors.rejectValue("description", "Error.dataFunction.description.length");
            }
        }
    }
}