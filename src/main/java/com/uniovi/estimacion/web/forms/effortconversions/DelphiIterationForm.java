package com.uniovi.estimacion.web.forms.effortconversions;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class DelphiIterationForm {

    private List<DelphiExpertEstimateForm> expertEstimates = new ArrayList<>();
}