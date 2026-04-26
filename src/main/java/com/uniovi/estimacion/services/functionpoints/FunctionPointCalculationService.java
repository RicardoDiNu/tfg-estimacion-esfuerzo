package com.uniovi.estimacion.services.functionpoints;

import com.uniovi.estimacion.entities.functionpoints.DataFunction;
import com.uniovi.estimacion.entities.functionpoints.DataFunctionType;
import com.uniovi.estimacion.entities.functionpoints.FunctionPointAnalysis;
import com.uniovi.estimacion.entities.functionpoints.FunctionPointComplexity;
import com.uniovi.estimacion.entities.functionpoints.GeneralSystemCharacteristicAssessment;
import com.uniovi.estimacion.entities.functionpoints.TransactionalFunction;
import com.uniovi.estimacion.entities.functionpoints.TransactionalFunctionType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FunctionPointCalculationService {

    public void recalculateAnalysis(FunctionPointAnalysis analysis) {
        int unadjustedFunctionPoints = 0;

        for (DataFunction dataFunction : analysis.getDataFunctions()) {
            int weight = calculateDataFunctionWeight(
                    dataFunction.getType(),
                    dataFunction.getComplexity()
            );

            dataFunction.setWeight(weight);
            unadjustedFunctionPoints += weight;
        }

        for (TransactionalFunction transactionalFunction : analysis.getTransactionalFunctions()) {
            int weight = calculateTransactionalFunctionWeight(
                    transactionalFunction.getType(),
                    transactionalFunction.getComplexity()
            );

            transactionalFunction.setWeight(weight);
            unadjustedFunctionPoints += weight;
        }

        int totalDegreeOfInfluence = analysis.getGeneralSystemCharacteristicAssessments()
                .stream()
                .mapToInt(GeneralSystemCharacteristicAssessment::getDegreeOfInfluence)
                .sum();

        double valueAdjustmentFactor = 0.65 + (0.01 * totalDegreeOfInfluence);
        double adjustedFunctionPoints = unadjustedFunctionPoints * valueAdjustmentFactor;

        analysis.setUnadjustedFunctionPoints(unadjustedFunctionPoints);
        analysis.setTotalDegreeOfInfluence(totalDegreeOfInfluence);
        analysis.setValueAdjustmentFactor(valueAdjustmentFactor);
        analysis.setAdjustedFunctionPoints(adjustedFunctionPoints);
    }

    public FunctionPointResults buildResults(FunctionPointAnalysis analysis) {
        recalculateAnalysis(analysis);

        List<Map<String, Object>> breakdownRows = buildBreakdownRows(analysis);

        int breakdownTotalSimple = breakdownRows.stream()
                .mapToInt(row -> (Integer) row.get("simpleCount"))
                .sum();

        int breakdownTotalAverage = breakdownRows.stream()
                .mapToInt(row -> (Integer) row.get("averageCount"))
                .sum();

        int breakdownTotalHigh = breakdownRows.stream()
                .mapToInt(row -> (Integer) row.get("highCount"))
                .sum();

        int breakdownTotalFunctions = breakdownRows.stream()
                .mapToInt(row -> (Integer) row.get("totalCount"))
                .sum();

        int breakdownTotalUfp = breakdownRows.stream()
                .mapToInt(row -> (Integer) row.get("ufpContribution"))
                .sum();

        return new FunctionPointResults(
                analysis.getUnadjustedFunctionPoints(),
                analysis.getTotalDegreeOfInfluence(),
                analysis.getValueAdjustmentFactor(),
                analysis.getAdjustedFunctionPoints(),
                breakdownRows,
                breakdownTotalSimple,
                breakdownTotalAverage,
                breakdownTotalHigh,
                breakdownTotalFunctions,
                breakdownTotalUfp
        );
    }

    public int calculateDataFunctionWeight(DataFunctionType type, FunctionPointComplexity complexity) {
        if (type == null || complexity == null) {
            return 0;
        }

        return switch (type) {
            case ILF -> switch (complexity) {
                case LOW -> 7;
                case AVERAGE -> 10;
                case HIGH -> 15;
            };
            case EIF -> switch (complexity) {
                case LOW -> 5;
                case AVERAGE -> 7;
                case HIGH -> 10;
            };
        };
    }

    public int calculateTransactionalFunctionWeight(
            TransactionalFunctionType type,
            FunctionPointComplexity complexity
    ) {
        if (type == null || complexity == null) {
            return 0;
        }

        return switch (type) {
            case EI -> switch (complexity) {
                case LOW -> 3;
                case AVERAGE -> 4;
                case HIGH -> 6;
            };
            case EO -> switch (complexity) {
                case LOW -> 4;
                case AVERAGE -> 5;
                case HIGH -> 7;
            };
            case EQ -> switch (complexity) {
                case LOW -> 3;
                case AVERAGE -> 4;
                case HIGH -> 6;
            };
        };
    }

    private List<Map<String, Object>> buildBreakdownRows(FunctionPointAnalysis analysis) {
        List<Map<String, Object>> rows = new ArrayList<>();

        rows.add(buildTransactionalFunctionRow(
                analysis,
                TransactionalFunctionType.EI,
                "fp.results.type.ei"
        ));

        rows.add(buildTransactionalFunctionRow(
                analysis,
                TransactionalFunctionType.EO,
                "fp.results.type.eo"
        ));

        rows.add(buildTransactionalFunctionRow(
                analysis,
                TransactionalFunctionType.EQ,
                "fp.results.type.eq"
        ));

        rows.add(buildDataFunctionRow(
                analysis,
                DataFunctionType.ILF,
                "fp.results.type.ilf"
        ));

        rows.add(buildDataFunctionRow(
                analysis,
                DataFunctionType.EIF,
                "fp.results.type.eif"
        ));

        return rows;
    }

    private Map<String, Object> buildDataFunctionRow(
            FunctionPointAnalysis analysis,
            DataFunctionType type,
            String labelKey
    ) {
        int simpleCount = (int) analysis.getDataFunctions().stream()
                .filter(f -> f.getType() == type && f.getComplexity() == FunctionPointComplexity.LOW)
                .count();

        int averageCount = (int) analysis.getDataFunctions().stream()
                .filter(f -> f.getType() == type && f.getComplexity() == FunctionPointComplexity.AVERAGE)
                .count();

        int highCount = (int) analysis.getDataFunctions().stream()
                .filter(f -> f.getType() == type && f.getComplexity() == FunctionPointComplexity.HIGH)
                .count();

        int totalCount = simpleCount + averageCount + highCount;
        int ufpContribution =
                simpleCount * calculateDataFunctionWeight(type, FunctionPointComplexity.LOW) +
                        averageCount * calculateDataFunctionWeight(type, FunctionPointComplexity.AVERAGE) +
                        highCount * calculateDataFunctionWeight(type, FunctionPointComplexity.HIGH);

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("labelKey", labelKey);
        row.put("simpleCount", simpleCount);
        row.put("averageCount", averageCount);
        row.put("highCount", highCount);
        row.put("totalCount", totalCount);
        row.put("ufpContribution", ufpContribution);

        return row;
    }

    private Map<String, Object> buildTransactionalFunctionRow(
            FunctionPointAnalysis analysis,
            TransactionalFunctionType type,
            String labelKey
    ) {
        int simpleCount = (int) analysis.getTransactionalFunctions().stream()
                .filter(f -> f.getType() == type && f.getComplexity() == FunctionPointComplexity.LOW)
                .count();

        int averageCount = (int) analysis.getTransactionalFunctions().stream()
                .filter(f -> f.getType() == type && f.getComplexity() == FunctionPointComplexity.AVERAGE)
                .count();

        int highCount = (int) analysis.getTransactionalFunctions().stream()
                .filter(f -> f.getType() == type && f.getComplexity() == FunctionPointComplexity.HIGH)
                .count();

        int totalCount = simpleCount + averageCount + highCount;
        int ufpContribution =
                simpleCount * calculateTransactionalFunctionWeight(type, FunctionPointComplexity.LOW) +
                        averageCount * calculateTransactionalFunctionWeight(type, FunctionPointComplexity.AVERAGE) +
                        highCount * calculateTransactionalFunctionWeight(type, FunctionPointComplexity.HIGH);

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("labelKey", labelKey);
        row.put("simpleCount", simpleCount);
        row.put("averageCount", averageCount);
        row.put("highCount", highCount);
        row.put("totalCount", totalCount);
        row.put("ufpContribution", ufpContribution);

        return row;
    }
}