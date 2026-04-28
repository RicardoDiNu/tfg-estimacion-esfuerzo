package com.uniovi.estimacion.web.forms.effortconversions;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DelphiExpertEstimateForm {

    private String evaluatorAlias;
    private Double minimumModuleEstimatedEffortHours;
    private Double maximumModuleEstimatedEffortHours;
    private String comments;
}