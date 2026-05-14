package com.uniovi.estimacion.services.sizeanalyses.functionpoints;

import java.util.List;
import java.util.Map;

public class FunctionPointAnalysisSummary {

    private final int unadjustedFunctionPoints;
    private final int totalDegreeOfInfluence;
    private final double valueAdjustmentFactor;
    private final double adjustedFunctionPoints;

    private final List<Map<String, Object>> breakdownRows;

    private final int breakdownTotalSimple;
    private final int breakdownTotalAverage;
    private final int breakdownTotalHigh;
    private final int breakdownTotalFunctions;
    private final int breakdownTotalUfp;

    public FunctionPointAnalysisSummary(
            int unadjustedFunctionPoints,
            int totalDegreeOfInfluence,
            double valueAdjustmentFactor,
            double adjustedFunctionPoints,
            List<Map<String, Object>> breakdownRows,
            int breakdownTotalSimple,
            int breakdownTotalAverage,
            int breakdownTotalHigh,
            int breakdownTotalFunctions,
            int breakdownTotalUfp
    ) {
        this.unadjustedFunctionPoints = unadjustedFunctionPoints;
        this.totalDegreeOfInfluence = totalDegreeOfInfluence;
        this.valueAdjustmentFactor = valueAdjustmentFactor;
        this.adjustedFunctionPoints = adjustedFunctionPoints;
        this.breakdownRows = breakdownRows;
        this.breakdownTotalSimple = breakdownTotalSimple;
        this.breakdownTotalAverage = breakdownTotalAverage;
        this.breakdownTotalHigh = breakdownTotalHigh;
        this.breakdownTotalFunctions = breakdownTotalFunctions;
        this.breakdownTotalUfp = breakdownTotalUfp;
    }

    public int getUnadjustedFunctionPoints() {
        return unadjustedFunctionPoints;
    }

    public int getTotalDegreeOfInfluence() {
        return totalDegreeOfInfluence;
    }

    public double getValueAdjustmentFactor() {
        return valueAdjustmentFactor;
    }

    public double getAdjustedFunctionPoints() {
        return adjustedFunctionPoints;
    }

    public List<Map<String, Object>> getBreakdownRows() {
        return breakdownRows;
    }

    public int getBreakdownTotalSimple() {
        return breakdownTotalSimple;
    }

    public int getBreakdownTotalAverage() {
        return breakdownTotalAverage;
    }

    public int getBreakdownTotalHigh() {
        return breakdownTotalHigh;
    }

    public int getBreakdownTotalFunctions() {
        return breakdownTotalFunctions;
    }

    public int getBreakdownTotalUfp() {
        return breakdownTotalUfp;
    }
}