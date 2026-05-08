package com.uniovi.estimacion.services.usecasepoints;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class UseCasePointAnalysisSummary {

    private final int unadjustedActorWeight;
    private final int unadjustedUseCaseWeight;
    private final int unadjustedUseCasePoints;

    private final double technicalFactor;
    private final double technicalComplexityFactor;

    private final double environmentalFactor;
    private final double environmentalComplexityFactor;

    private final double adjustedUseCasePoints;

    private final List<Map<String, Object>> actorBreakdownRows;
    private final List<Map<String, Object>> useCaseBreakdownRows;

    private final int actorBreakdownTotalSimple;
    private final int actorBreakdownTotalAverage;
    private final int actorBreakdownTotalComplex;
    private final int actorBreakdownTotalActors;
    private final int actorBreakdownTotalWeight;

    private final int useCaseBreakdownTotalSimple;
    private final int useCaseBreakdownTotalAverage;
    private final int useCaseBreakdownTotalComplex;
    private final int useCaseBreakdownTotalUseCases;
    private final int useCaseBreakdownTotalWeight;
}