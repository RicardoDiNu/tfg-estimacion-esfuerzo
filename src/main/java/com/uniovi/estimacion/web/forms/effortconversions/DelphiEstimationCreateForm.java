package com.uniovi.estimacion.web.forms.effortconversions;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DelphiEstimationCreateForm {

    private Double confidencePercentage = 95.0;
    private Double acceptableDeviationPercentage = 10.0;
    private Integer maximumIterations = 2;
}