package com.uniovi.estimacion.web.forms.effortconversions.delphi;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DelphiEstimationCreateForm {

    private Double acceptableDeviationPercentage = 10.0;
    private Integer maximumIterations = 2;
    private Integer expertCount = 3;
}