package com.uniovi.estimacion.validators.sizeanalyses.functionpoints;

import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.FunctionPointAnalysis;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.gscs.GeneralSystemCharacteristicAssessment;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.gscs.GeneralSystemCharacteristicType;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Component
public class GeneralSystemCharacteristicsValidator implements Validator {

    private static final int CUSTOM_TEXT_MAX_LENGTH = 500;

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

        if (assessments.size() != GeneralSystemCharacteristicType.values().length) {
            errors.reject("Error.gsc.invalidCount");
            return;
        }

        Set<GeneralSystemCharacteristicType> receivedTypes =
                EnumSet.noneOf(GeneralSystemCharacteristicType.class);

        for (int i = 0; i < assessments.size(); i++) {
            GeneralSystemCharacteristicAssessment assessment = assessments.get(i);

            if (assessment == null || assessment.getCharacteristicType() == null) {
                errors.rejectValue(
                        "generalSystemCharacteristicAssessments[" + i + "].characteristicType",
                        "Error.gsc.characteristicType.empty"
                );
                continue;
            }

            if (!receivedTypes.add(assessment.getCharacteristicType())) {
                errors.rejectValue(
                        "generalSystemCharacteristicAssessments[" + i + "].characteristicType",
                        "Error.gsc.characteristicType.duplicated"
                );
            }

            Integer degree = assessment.getDegreeOfInfluence();

            if (degree == null || degree < 0 || degree > 5) {
                errors.rejectValue(
                        "generalSystemCharacteristicAssessments[" + i + "].degreeOfInfluence",
                        "Error.gsc.degree.range"
                );
            }

            String customText = assessment.getCustomText();

            if (customText != null && customText.length() > CUSTOM_TEXT_MAX_LENGTH) {
                errors.rejectValue(
                        "generalSystemCharacteristicAssessments[" + i + "].customText",
                        "Error.gsc.customText.length"
                );
            }
        }
    }
}