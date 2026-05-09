package com.uniovi.estimacion.services.sizeanalyses.functionpoints;

import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.FunctionPointAnalysis;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.DataFunction;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.DataFunctionType;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.FunctionPointComplexity;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.gscs.GeneralSystemCharacteristicAssessment;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.TransactionalFunction;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.TransactionalFunctionType;
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

    public FunctionPointAnalysisSummary buildSummary(FunctionPointAnalysis analysis) {
        recalculateAnalysis(analysis);

        return buildSummary(
                analysis.getDataFunctions(),
                analysis.getTransactionalFunctions(),
                analysis.getTotalDegreeOfInfluence(),
                analysis.getValueAdjustmentFactor()
        );
    }

    public FunctionPointAnalysisSummary buildModuleSummary(FunctionPointAnalysis analysis,
                                                           List<DataFunction> dataFunctions,
                                                           List<TransactionalFunction> transactionalFunctions) {
        return buildSummary(
                dataFunctions,
                transactionalFunctions,
                analysis.getTotalDegreeOfInfluence(),
                analysis.getValueAdjustmentFactor()
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

    public int calculateTransactionalFunctionWeight(TransactionalFunctionType type,
                                                    FunctionPointComplexity complexity) {
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

    private FunctionPointAnalysisSummary buildSummary(List<DataFunction> dataFunctions,
                                                      List<TransactionalFunction> transactionalFunctions,
                                                      Integer totalDegreeOfInfluence,
                                                      Double valueAdjustmentFactor) {
        int unadjustedFunctionPoints = calculateUnadjustedFunctionPoints(dataFunctions, transactionalFunctions);

        int tdi = totalDegreeOfInfluence != null ? totalDegreeOfInfluence : 0;
        double vaf = valueAdjustmentFactor != null ? valueAdjustmentFactor : 0.65 + (0.01 * tdi);
        double adjustedFunctionPoints = unadjustedFunctionPoints * vaf;

        List<Map<String, Object>> breakdownRows = buildBreakdownRows(dataFunctions, transactionalFunctions);

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

        return new FunctionPointAnalysisSummary(
                unadjustedFunctionPoints,
                tdi,
                vaf,
                adjustedFunctionPoints,
                breakdownRows,
                breakdownTotalSimple,
                breakdownTotalAverage,
                breakdownTotalHigh,
                breakdownTotalFunctions,
                breakdownTotalUfp
        );
    }

    private int calculateUnadjustedFunctionPoints(List<DataFunction> dataFunctions,
                                                  List<TransactionalFunction> transactionalFunctions) {
        int unadjustedFunctionPoints = 0;

        for (DataFunction dataFunction : dataFunctions) {
            unadjustedFunctionPoints += calculateDataFunctionWeight(
                    dataFunction.getType(),
                    dataFunction.getComplexity()
            );
        }

        for (TransactionalFunction transactionalFunction : transactionalFunctions) {
            unadjustedFunctionPoints += calculateTransactionalFunctionWeight(
                    transactionalFunction.getType(),
                    transactionalFunction.getComplexity()
            );
        }

        return unadjustedFunctionPoints;
    }

    private List<Map<String, Object>> buildBreakdownRows(List<DataFunction> dataFunctions,
                                                         List<TransactionalFunction> transactionalFunctions) {
        List<Map<String, Object>> rows = new ArrayList<>();

        rows.add(buildTransactionalFunctionRow(
                transactionalFunctions,
                TransactionalFunctionType.EI,
                "fp.results.type.ei"
        ));

        rows.add(buildTransactionalFunctionRow(
                transactionalFunctions,
                TransactionalFunctionType.EO,
                "fp.results.type.eo"
        ));

        rows.add(buildTransactionalFunctionRow(
                transactionalFunctions,
                TransactionalFunctionType.EQ,
                "fp.results.type.eq"
        ));

        rows.add(buildDataFunctionRow(
                dataFunctions,
                DataFunctionType.ILF,
                "fp.results.type.ilf"
        ));

        rows.add(buildDataFunctionRow(
                dataFunctions,
                DataFunctionType.EIF,
                "fp.results.type.eif"
        ));

        return rows;
    }

    private Map<String, Object> buildDataFunctionRow(List<DataFunction> dataFunctions,
                                                     DataFunctionType type,
                                                     String labelKey) {
        int simpleCount = (int) dataFunctions.stream()
                .filter(f -> f.getType() == type && f.getComplexity() == FunctionPointComplexity.LOW)
                .count();

        int averageCount = (int) dataFunctions.stream()
                .filter(f -> f.getType() == type && f.getComplexity() == FunctionPointComplexity.AVERAGE)
                .count();

        int highCount = (int) dataFunctions.stream()
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

    private Map<String, Object> buildTransactionalFunctionRow(List<TransactionalFunction> transactionalFunctions,
                                                              TransactionalFunctionType type,
                                                              String labelKey) {
        int simpleCount = (int) transactionalFunctions.stream()
                .filter(f -> f.getType() == type && f.getComplexity() == FunctionPointComplexity.LOW)
                .count();

        int averageCount = (int) transactionalFunctions.stream()
                .filter(f -> f.getType() == type && f.getComplexity() == FunctionPointComplexity.AVERAGE)
                .count();

        int highCount = (int) transactionalFunctions.stream()
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