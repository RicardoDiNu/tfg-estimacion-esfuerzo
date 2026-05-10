package com.uniovi.estimacion.web.dtos.reports.usecasepoints;

import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.modules.UseCasePointModule;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.usecases.UseCaseEntry;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class UseCasePointModuleReportRow {

    private UseCasePointModule module;

    private List<UseCaseEntry> useCases;

    private Integer useCaseWeight;

    private Double unadjustedUseCasePoints;

    private Double adjustedUseCasePoints;
}