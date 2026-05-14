package com.uniovi.estimacion.services.sizeanalyses;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SizeAnalysisModuleResult {

    private final Long moduleId;

    private final String moduleName;

    private final Double size;
}