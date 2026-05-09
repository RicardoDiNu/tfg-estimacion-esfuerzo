package com.uniovi.estimacion.validators.effortconversions.delphi;

import com.uniovi.estimacion.web.forms.effortconversions.delphi.DelphiExpertEstimateForm;
import com.uniovi.estimacion.web.forms.effortconversions.delphi.DelphiIterationForm;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class DelphiIterationValidator implements Validator {

    private static final int MINIMUM_EXPERT_COUNT = 3;

    @Override
    public boolean supports(Class<?> clazz) {
        return DelphiIterationForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        DelphiIterationForm form = (DelphiIterationForm) target;

        Integer expectedExpertCount = form.getExpectedExpertCount();
        List<DelphiExpertEstimateForm> expertEstimates = form.getExpertEstimates();

        validateExpectedExpertCount(expectedExpertCount, errors);
        validateExpertEstimateCollection(expectedExpertCount, expertEstimates, errors);

        if (errors.hasErrors()) {
            return;
        }

        validateExpertEstimates(expertEstimates, errors);
    }

    private void validateExpectedExpertCount(Integer expectedExpertCount, Errors errors) {
        if (expectedExpertCount == null) {
            errors.reject(
                    "delphi.validation.expectedExpertCount.required"
            );
            return;
        }

        if (expectedExpertCount < MINIMUM_EXPERT_COUNT) {
            errors.reject(
                    "delphi.validation.expectedExpertCount.min",
                    new Object[]{MINIMUM_EXPERT_COUNT},
                    null
            );
        }
    }

    private void validateExpertEstimateCollection(Integer expectedExpertCount,
                                                  List<DelphiExpertEstimateForm> expertEstimates,
                                                  Errors errors) {
        if (expertEstimates == null || expertEstimates.isEmpty()) {
            errors.reject(
                    "delphi.validation.expertEstimates.required"
            );
            return;
        }

        if (expectedExpertCount != null && expertEstimates.size() != expectedExpertCount) {
            errors.reject(
                    "delphi.validation.expertEstimates.count",
                    new Object[]{expectedExpertCount},
                    null
            );
        }
    }

    private void validateExpertEstimates(List<DelphiExpertEstimateForm> expertEstimates, Errors errors) {
        Set<String> aliases = new HashSet<>();

        for (int i = 0; i < expertEstimates.size(); i++) {
            DelphiExpertEstimateForm expertEstimate = expertEstimates.get(i);

            if (expertEstimate == null) {
                errors.rejectValue(
                        "expertEstimates[" + i + "]",
                        "delphi.validation.expertEstimates.row.required"
                );
                continue;
            }

            validateEvaluatorAlias(expertEstimate, i, aliases, errors);
            validateMinimumModuleEstimatedEffortHours(expertEstimate, i, errors);
            validateMaximumModuleEstimatedEffortHours(expertEstimate, i, errors);
        }
    }

    private void validateEvaluatorAlias(DelphiExpertEstimateForm expertEstimate,
                                        int index,
                                        Set<String> aliases,
                                        Errors errors) {
        String evaluatorAlias = normalize(expertEstimate.getEvaluatorAlias());

        if (!StringUtils.hasText(evaluatorAlias)) {
            errors.rejectValue(
                    "expertEstimates[" + index + "].evaluatorAlias",
                    "delphi.validation.expertEstimate.evaluatorAlias.required"
            );
            return;
        }

        String normalizedAliasKey = evaluatorAlias.toLowerCase();

        if (!aliases.add(normalizedAliasKey)) {
            errors.rejectValue(
                    "expertEstimates[" + index + "].evaluatorAlias",
                    "delphi.validation.expertEstimate.evaluatorAlias.duplicate"
            );
        }
    }

    private void validateMinimumModuleEstimatedEffortHours(DelphiExpertEstimateForm expertEstimate,
                                                           int index,
                                                           Errors errors) {
        Double value = expertEstimate.getMinimumModuleEstimatedEffortHours();

        if (value == null) {
            errors.rejectValue(
                    "expertEstimates[" + index + "].minimumModuleEstimatedEffortHours",
                    "delphi.validation.expertEstimate.minimumModuleEstimatedEffortHours.required"
            );
            return;
        }

        if (value <= 0) {
            errors.rejectValue(
                    "expertEstimates[" + index + "].minimumModuleEstimatedEffortHours",
                    "delphi.validation.expertEstimate.minimumModuleEstimatedEffortHours.positive"
            );
        }
    }

    private void validateMaximumModuleEstimatedEffortHours(DelphiExpertEstimateForm expertEstimate,
                                                           int index,
                                                           Errors errors) {
        Double value = expertEstimate.getMaximumModuleEstimatedEffortHours();

        if (value == null) {
            errors.rejectValue(
                    "expertEstimates[" + index + "].maximumModuleEstimatedEffortHours",
                    "delphi.validation.expertEstimate.maximumModuleEstimatedEffortHours.required"
            );
            return;
        }

        if (value <= 0) {
            errors.rejectValue(
                    "expertEstimates[" + index + "].maximumModuleEstimatedEffortHours",
                    "delphi.validation.expertEstimate.maximumModuleEstimatedEffortHours.positive"
            );
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}