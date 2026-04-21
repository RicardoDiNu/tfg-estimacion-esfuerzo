package com.uniovi.estimacion.validators.functionpoints;

import com.uniovi.estimacion.entities.functionpoints.FunctionPointAnalysis;
import com.uniovi.estimacion.entities.functionpoints.GeneralSystemCharacteristicAssessment;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.List;

@Component
public class GeneralSystemCharacteristicsValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return FunctionPointAnalysis.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        FunctionPointAnalysis analysis = (FunctionPointAnalysis) target;

        List<GeneralSystemCharacteristicAssessment> assessments =
                analysis.getGeneralSystemCharacteristicAssessments();

        if (assessments == null || assessments.isEmpty()) {
            errors.reject("Error.gsc.empty");
            return;
        }

        for (int i = 0; i < assessments.size(); i++) {
            GeneralSystemCharacteristicAssessment assessment = assessments.get(i);
            Integer degree = assessment.getDegreeOfInfluence();

            if (degree == null || degree < 0 || degree > 5) {
                errors.rejectValue(
                        "generalSystemCharacteristicAssessments[" + i + "].degreeOfInfluence",
                        "Error.gsc.degree.range"
                );
            }
        }
    }
}