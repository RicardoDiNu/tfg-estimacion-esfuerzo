package com.uniovi.estimacion.web.forms.sizeanalyses.usecasepoints;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class UseCaseTechnicalFactorsForm {

    private List<UseCaseFactorAssessmentForm> assessments = new ArrayList<>();
}