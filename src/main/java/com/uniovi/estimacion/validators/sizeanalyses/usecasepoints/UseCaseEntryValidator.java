package com.uniovi.estimacion.validators.sizeanalyses.usecasepoints;

import com.uniovi.estimacion.web.forms.sizeanalyses.usecasepoints.UseCaseEntryForm;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class UseCaseEntryValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return UseCaseEntryForm.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        UseCaseEntryForm form = (UseCaseEntryForm) target;

        if (form.getName() == null || form.getName().trim().isEmpty()) {
            errors.rejectValue("name", "ucp.useCase.validation.name.empty");
        } else if (form.getName().trim().length() > 150) {
            errors.rejectValue("name", "ucp.useCase.validation.name.tooLong");
        }

        if (form.getDescription() != null && form.getDescription().length() > 1000) {
            errors.rejectValue("description", "ucp.useCase.validation.description.tooLong");
        }

        if (form.getActorIds() == null || form.getActorIds().isEmpty()) {
            errors.rejectValue("actorIds", "ucp.useCase.validation.actors.empty");
        }

        if (form.getTriggerCondition() != null && form.getTriggerCondition().length() > 1000) {
            errors.rejectValue("triggerCondition", "ucp.useCase.validation.triggerCondition.tooLong");
        }

        if (form.getPreconditions() != null && form.getPreconditions().length() > 2000) {
            errors.rejectValue("preconditions", "ucp.useCase.validation.preconditions.tooLong");
        }

        if (form.getPostconditions() != null && form.getPostconditions().length() > 2000) {
            errors.rejectValue("postconditions", "ucp.useCase.validation.postconditions.tooLong");
        }

        if (form.getNormalFlow() != null && form.getNormalFlow().length() > 4000) {
            errors.rejectValue("normalFlow", "ucp.useCase.validation.normalFlow.tooLong");
        }

        if (form.getAlternativeFlows() != null && form.getAlternativeFlows().length() > 4000) {
            errors.rejectValue("alternativeFlows", "ucp.useCase.validation.alternativeFlows.tooLong");
        }

        if (form.getExceptionFlows() != null && form.getExceptionFlows().length() > 2000) {
            errors.rejectValue("exceptionFlows", "ucp.useCase.validation.exceptionFlows.tooLong");
        }

        if (form.getTransactionCount() == null) {
            errors.rejectValue("transactionCount", "ucp.useCase.validation.transactionCount.empty");
        } else if (form.getTransactionCount() <= 0) {
            errors.rejectValue("transactionCount", "ucp.useCase.validation.transactionCount.positive");
        } else if (form.getTransactionCount() > 999) {
            errors.rejectValue("transactionCount", "ucp.useCase.validation.transactionCount.tooHigh");
        }
    }
}