package com.uniovi.estimacion.web.forms.usecasepoints;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class UseCaseEnvironmentalFactorsForm {

    private List<UseCaseFactorAssessmentForm> assessments = new ArrayList<>();
}