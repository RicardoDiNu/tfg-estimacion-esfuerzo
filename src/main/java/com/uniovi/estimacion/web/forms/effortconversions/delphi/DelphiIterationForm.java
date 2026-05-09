package com.uniovi.estimacion.web.forms.effortconversions.delphi;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class DelphiIterationForm {

    private Integer expectedExpertCount = 3;
    private List<DelphiExpertEstimateForm> expertEstimates = new ArrayList<>();
}