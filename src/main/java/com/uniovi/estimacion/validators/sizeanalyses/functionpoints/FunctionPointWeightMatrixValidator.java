package com.uniovi.estimacion.validators.sizeanalyses.functionpoints;

import com.uniovi.estimacion.web.forms.sizeanalyses.functionpoints.FunctionPointWeightMatrixForm;
import com.uniovi.estimacion.web.forms.sizeanalyses.functionpoints.FunctionPointWeightMatrixRowForm;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class FunctionPointWeightMatrixValidator implements Validator {

    private static final int MIN_WEIGHT = 1;
    private static final int MAX_WEIGHT = 999;

    @Override
    public boolean supports(Class<?> clazz) {
        return FunctionPointWeightMatrixForm.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        FunctionPointWeightMatrixForm form = (FunctionPointWeightMatrixForm) target;

        if (form.getRows() == null || form.getRows().isEmpty()) {
            errors.reject("Error.fpWeightMatrix.empty");
            return;
        }

        for (int i = 0; i < form.getRows().size(); i++) {
            FunctionPointWeightMatrixRowForm row = form.getRows().get(i);

            if (row.getFunctionType() == null) {
                errors.rejectValue(
                        "rows[" + i + "].functionType",
                        "Error.fpWeightMatrix.functionType.empty"
                );
            }

            validateWeight(errors, i, "lowWeight", row.getLowWeight());
            validateWeight(errors, i, "averageWeight", row.getAverageWeight());
            validateWeight(errors, i, "highWeight", row.getHighWeight());

            if (row.getLowWeight() != null
                    && row.getAverageWeight() != null
                    && row.getHighWeight() != null
                    && row.getLowWeight() >= MIN_WEIGHT
                    && row.getAverageWeight() >= MIN_WEIGHT
                    && row.getHighWeight() >= MIN_WEIGHT) {

                if (row.getLowWeight() > row.getAverageWeight()) {
                    errors.rejectValue(
                            "rows[" + i + "].lowWeight",
                            "Error.fpWeightMatrix.order.lowAverage"
                    );
                }

                if (row.getAverageWeight() > row.getHighWeight()) {
                    errors.rejectValue(
                            "rows[" + i + "].averageWeight",
                            "Error.fpWeightMatrix.order.averageHigh"
                    );
                }
            }
        }
    }

    private void validateWeight(Errors errors, int rowIndex, String fieldName, Integer value) {
        if (value == null) {
            errors.rejectValue(
                    "rows[" + rowIndex + "]." + fieldName,
                    "Error.fpWeightMatrix.weight.empty"
            );
            return;
        }

        if (value < MIN_WEIGHT || value > MAX_WEIGHT) {
            errors.rejectValue(
                    "rows[" + rowIndex + "]." + fieldName,
                    "Error.fpWeightMatrix.weight.range"
            );
        }
    }
}