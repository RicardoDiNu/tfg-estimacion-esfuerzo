package com.uniovi.estimacion.validators.sizeanalyses.functionpoints;

import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.TransactionalFunction;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

@Component
public class TransactionalFunctionValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return TransactionalFunction.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        TransactionalFunction transactionalFunction = (TransactionalFunction) target;

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "type", "Error.empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "complexity", "Error.empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "Error.empty");

        if (transactionalFunction.getName() != null) {
            String name = transactionalFunction.getName().trim();

            if (name.isEmpty() || name.length() > 100) {
                errors.rejectValue("name", "Error.transactionalFunction.name.length");
            }
        }

        if (transactionalFunction.getDescription() != null) {
            String description = transactionalFunction.getDescription().trim();

            if (description.length() > 1000) {
                errors.rejectValue("description", "Error.transactionalFunction.description.length");
            }
        }
    }
}