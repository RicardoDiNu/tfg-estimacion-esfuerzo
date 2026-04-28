package com.uniovi.estimacion.validators.effortconversions;

import com.uniovi.estimacion.web.forms.effortconversions.DelphiEstimationCreateForm;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class DelphiEstimationValidator implements Validator {

    private static final double MIN_CONFIDENCE = 0.0;
    private static final double MAX_CONFIDENCE = 100.0;
    private static final double MIN_ACCEPTABLE_DEVIATION = 0.0;
    private static final int MIN_MAXIMUM_ITERATIONS = 1;

    @Override
    public boolean supports(Class<?> clazz) {
        return DelphiEstimationCreateForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        DelphiEstimationCreateForm form = (DelphiEstimationCreateForm) target;

        validateConfidencePercentage(form.getConfidencePercentage(), errors);
        validateAcceptableDeviationPercentage(form.getAcceptableDeviationPercentage(), errors);
        validateMaximumIterations(form.getMaximumIterations(), errors);
    }

    private void validateConfidencePercentage(Double confidencePercentage, Errors errors) {
        if (confidencePercentage == null) {
            errors.rejectValue(
                    "confidencePercentage",
                    "delphi.validation.confidence.required"
            );
            return;
        }

        if (confidencePercentage < MIN_CONFIDENCE || confidencePercentage > MAX_CONFIDENCE) {
            errors.rejectValue(
                    "confidencePercentage",
                    "delphi.validation.confidence.range"
            );
        }
    }

    private void validateAcceptableDeviationPercentage(Double acceptableDeviationPercentage, Errors errors) {
        if (acceptableDeviationPercentage == null) {
            errors.rejectValue(
                    "acceptableDeviationPercentage",
                    "delphi.validation.acceptableDeviation.required"
            );
            return;
        }

        if (acceptableDeviationPercentage <= MIN_ACCEPTABLE_DEVIATION) {
            errors.rejectValue(
                    "acceptableDeviationPercentage",
                    "delphi.validation.acceptableDeviation.positive"
            );
        }
    }

    private void validateMaximumIterations(Integer maximumIterations, Errors errors) {
        if (maximumIterations == null) {
            errors.rejectValue(
                    "maximumIterations",
                    "delphi.validation.maximumIterations.required"
            );
            return;
        }

        if (maximumIterations < MIN_MAXIMUM_ITERATIONS) {
            errors.rejectValue(
                    "maximumIterations",
                    "delphi.validation.maximumIterations.min"
            );
        }
    }
}