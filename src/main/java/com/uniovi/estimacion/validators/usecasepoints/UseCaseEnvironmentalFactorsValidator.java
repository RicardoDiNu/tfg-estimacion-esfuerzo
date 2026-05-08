package com.uniovi.estimacion.validators.usecasepoints;

import com.uniovi.estimacion.web.forms.usecasepoints.UseCaseEnvironmentalFactorsForm;
import com.uniovi.estimacion.web.forms.usecasepoints.UseCaseFactorAssessmentForm;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class UseCaseEnvironmentalFactorsValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return UseCaseEnvironmentalFactorsForm.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        UseCaseEnvironmentalFactorsForm form = (UseCaseEnvironmentalFactorsForm) target;

        if (form.getAssessments() == null || form.getAssessments().isEmpty()) {
            errors.reject("ucp.environmentalFactors.validation.empty");
            return;
        }

        for (int i = 0; i < form.getAssessments().size(); i++) {
            UseCaseFactorAssessmentForm assessment = form.getAssessments().get(i);

            if (assessment.getFactorCode() == null || assessment.getFactorCode().trim().isEmpty()) {
                errors.rejectValue(
                        "assessments[" + i + "].factorCode",
                        "ucp.factors.validation.factorCode.empty"
                );
            }

            if (assessment.getDegreeOfInfluence() == null) {
                errors.rejectValue(
                        "assessments[" + i + "].degreeOfInfluence",
                        "ucp.factors.validation.degree.empty"
                );
            } else if (assessment.getDegreeOfInfluence() < 0 || assessment.getDegreeOfInfluence() > 5) {
                errors.rejectValue(
                        "assessments[" + i + "].degreeOfInfluence",
                        "ucp.factors.validation.degree.range"
                );
            }
        }
    }
}