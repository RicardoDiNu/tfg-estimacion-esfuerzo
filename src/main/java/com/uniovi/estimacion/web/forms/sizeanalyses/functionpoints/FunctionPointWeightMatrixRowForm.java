package com.uniovi.estimacion.web.forms.sizeanalyses.functionpoints;

import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.weights.FunctionPointFunctionType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FunctionPointWeightMatrixRowForm {

    private FunctionPointFunctionType functionType;

    private Integer lowWeight;

    private Integer averageWeight;

    private Integer highWeight;
}