package com.uniovi.estimacion.validators.effortconversions;

import com.uniovi.estimacion.web.forms.effortconversions.DelphiExpertEstimateForm;
import com.uniovi.estimacion.web.forms.effortconversions.DelphiIterationForm;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class DelphiIterationValidator implements Validator {

    private static final int MIN_EXPERTS = 3;

    @Override
    public boolean supports(Class<?> clazz) {
        return DelphiIterationForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        DelphiIterationForm form = (DelphiIterationForm) target;

        List<DelphiExpertEstimateForm> expertEstimates = form.getExpertEstimates();

        if (expertEstimates == null || expertEstimates.size() < MIN_EXPERTS) {
            errors.reject(
                    "delphi.validation.expertEstimates.minimum"
            );
            return;
        }

        validateExpertEstimates(expertEstimates, errors);
    }

    private void validateExpertEstimates(List<DelphiExpertEstimateForm> expertEstimates, Errors errors) {
        Set<String> normalizedAliases = new HashSet<>();

        for (int i = 0; i < expertEstimates.size(); i++) {
            DelphiExpertEstimateForm expertEstimate = expertEstimates.get(i);
            String fieldPrefix = "expertEstimates[" + i + "]";

            validateEvaluatorAlias(expertEstimate, fieldPrefix, normalizedAliases, errors);
            validateMinimumModuleEstimatedEffortHours(expertEstimate, fieldPrefix, errors);
            validateMaximumModuleEstimatedEffortHours(expertEstimate, fieldPrefix, errors);
        }
    }

    private void validateEvaluatorAlias(DelphiExpertEstimateForm expertEstimate,
                                        String fieldPrefix,
                                        Set<String> normalizedAliases,
                                        Errors errors) {
        String alias = normalize(expertEstimate.getEvaluatorAlias());

        if (alias == null) {
            errors.rejectValue(
                    fieldPrefix + ".evaluatorAlias",
                    "delphi.validation.evaluatorAlias.required"
            );
            return;
        }

        String normalizedAlias = alias.toLowerCase();

        if (!normalizedAliases.add(normalizedAlias)) {
            errors.rejectValue(
                    fieldPrefix + ".evaluatorAlias",
                    "delphi.validation.evaluatorAlias.duplicated"
            );
        }
    }

    private void validateMinimumModuleEstimatedEffortHours(DelphiExpertEstimateForm expertEstimate,
                                                           String fieldPrefix,
                                                           Errors errors) {
        Double value = expertEstimate.getMinimumModuleEstimatedEffortHours();

        if (value == null) {
            errors.rejectValue(
                    fieldPrefix + ".minimumModuleEstimatedEffortHours",
                    "delphi.validation.minimumModuleEstimatedEffortHours.required"
            );
            return;
        }

        if (value <= 0) {
            errors.rejectValue(
                    fieldPrefix + ".minimumModuleEstimatedEffortHours",
                    "delphi.validation.minimumModuleEstimatedEffortHours.positive"
            );
        }
    }

    private void validateMaximumModuleEstimatedEffortHours(DelphiExpertEstimateForm expertEstimate,
                                                           String fieldPrefix,
                                                           Errors errors) {
        Double value = expertEstimate.getMaximumModuleEstimatedEffortHours();

        if (value == null) {
            errors.rejectValue(
                    fieldPrefix + ".maximumModuleEstimatedEffortHours",
                    "delphi.validation.maximumModuleEstimatedEffortHours.required"
            );
            return;
        }

        if (value <= 0) {
            errors.rejectValue(
                    fieldPrefix + ".maximumModuleEstimatedEffortHours",
                    "delphi.validation.maximumModuleEstimatedEffortHours.positive"
            );
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}