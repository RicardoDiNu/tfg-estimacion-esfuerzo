package com.uniovi.estimacion.web.forms.sizeanalyses.functionpoints;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class FunctionPointWeightMatrixForm {

    private List<FunctionPointWeightMatrixRowForm> rows = new ArrayList<>();
}