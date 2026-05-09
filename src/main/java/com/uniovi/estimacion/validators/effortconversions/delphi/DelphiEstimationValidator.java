package com.uniovi.estimacion.validators.effortconversions.delphi;

import com.uniovi.estimacion.web.forms.effortconversions.delphi.DelphiEstimationCreateForm;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class DelphiEstimationValidator implements Validator {

    private static final int MINIMUM_EXPERT_COUNT = 3;

    @Override
    public boolean supports(Class<?> clazz) {
        return DelphiEstimationCreateForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        DelphiEstimationCreateForm form = (DelphiEstimationCreateForm) target;

        validateAcceptableDeviationPercentage(form, errors);
        validateMaximumIterations(form, errors);
        validateExpertCount(form, errors);
    }

    private void validateAcceptableDeviationPercentage(DelphiEstimationCreateForm form, Errors errors) {
        Double acceptableDeviationPercentage = form.getAcceptableDeviationPercentage();

        if (acceptableDeviationPercentage == null) {
            errors.rejectValue(
                    "acceptableDeviationPercentage",
                    "delphi.validation.acceptableDeviationPercentage.required"
            );
            return;
        }

        if (acceptableDeviationPercentage <= 0 || acceptableDeviationPercentage > 100) {
            errors.rejectValue(
                    "acceptableDeviationPercentage",
                    "delphi.validation.acceptableDeviationPercentage.range"
            );
        }
    }

    private void validateMaximumIterations(DelphiEstimationCreateForm form, Errors errors) {
        Integer maximumIterations = form.getMaximumIterations();

        if (maximumIterations == null) {
            errors.rejectValue(
                    "maximumIterations",
                    "delphi.validation.maximumIterations.required"
            );
            return;
        }

        if (maximumIterations < 1) {
            errors.rejectValue(
                    "maximumIterations",
                    "delphi.validation.maximumIterations.min"
            );
        }
    }

    private void validateExpertCount(DelphiEstimationCreateForm form, Errors errors) {
        Integer expertCount = form.getExpertCount();

        if (expertCount == null) {
            errors.rejectValue(
                    "expertCount",
                    "delphi.validation.expertCount.required"
            );
            return;
        }

        if (expertCount < MINIMUM_EXPERT_COUNT) {
            errors.rejectValue(
                    "expertCount",
                    "delphi.validation.expertCount.min",
                    new Object[]{MINIMUM_EXPERT_COUNT},
                    null
            );
        }
    }
}