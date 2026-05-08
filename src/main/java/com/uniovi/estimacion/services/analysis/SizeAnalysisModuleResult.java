package com.uniovi.estimacion.services.analysis;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SizeAnalysisModuleResult {

    private final Long moduleId;

    private final String moduleName;

    private final Double size;
}