package com.uniovi.estimacion.validators.requirements;

import com.uniovi.estimacion.entities.requirements.UserRequirement;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

@Component
public class UserRequirementValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return UserRequirement.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        UserRequirement requirement = (UserRequirement) target;

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "identifier", "Error.empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "statement", "Error.empty");

        if (requirement.getIdentifier() != null) {
            String identifier = requirement.getIdentifier().trim();

            if (identifier.length() < 2 || identifier.length() > 50) {
                errors.rejectValue("identifier", "Error.requirement.identifier.length");
            }
        }

        if (requirement.getStatement() != null) {
            String statement = requirement.getStatement().trim();

            if (statement.length() < 5 || statement.length() > 1000) {
                errors.rejectValue("statement", "Error.requirement.statement.length");
            }
        }
    }
}