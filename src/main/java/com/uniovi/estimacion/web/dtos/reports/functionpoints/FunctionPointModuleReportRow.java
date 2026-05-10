package com.uniovi.estimacion.web.dtos.reports.functionpoints;

import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.DataFunction;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.TransactionalFunction;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.modules.FunctionPointModule;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.requirements.UserRequirement;
import com.uniovi.estimacion.services.sizeanalyses.functionpoints.FunctionPointAnalysisSummary;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class FunctionPointModuleReportRow {

    private FunctionPointModule module;

    private FunctionPointAnalysisSummary results;

    private List<DataFunction> dataFunctions;

    private List<TransactionalFunction> transactionalFunctions;

    private List<UserRequirement> requirements;
}