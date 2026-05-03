package com.uniovi.estimacion.web.forms.effortconversions;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransformationFunctionForm {

    private String name;

    private String description;

    private Double intercept;

    private Double slope;
}