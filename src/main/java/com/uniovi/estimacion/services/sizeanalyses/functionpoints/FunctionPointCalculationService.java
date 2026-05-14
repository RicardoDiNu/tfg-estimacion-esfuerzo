package com.uniovi.estimacion.services.sizeanalyses.functionpoints;

import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.FunctionPointAnalysis;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.*;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.gscs.GeneralSystemCharacteristicAssessment;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.weights.FunctionPointFunctionType;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.weights.FunctionPointWeightMatrixEntry;
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
                    analysis,
                    dataFunction.getType(),
                    dataFunction.getComplexity()
            );

            dataFunction.setWeight(weight);
            unadjustedFunctionPoints += weight;
        }

        for (TransactionalFunction transactionalFunction : analysis.getTransactionalFunctions()) {
            int weight = calculateTransactionalFunctionWeight(
                    analysis,
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
                analysis,
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
                analysis,
                dataFunctions,
                transactionalFunctions,
                analysis.getTotalDegreeOfInfluence(),
                analysis.getValueAdjustmentFactor()
        );
    }

    public int calculateDataFunctionWeight(FunctionPointAnalysis analysis,
                                           DataFunctionType type,
                                           FunctionPointComplexity complexity) {
        FunctionPointFunctionType functionType =
                FunctionPointFunctionType.fromDataFunctionType(type);

        return resolveWeight(analysis, functionType, complexity);
    }

    public int calculateTransactionalFunctionWeight(FunctionPointAnalysis analysis,
                                                    TransactionalFunctionType type,
                                                    FunctionPointComplexity complexity) {
        FunctionPointFunctionType functionType =
                FunctionPointFunctionType.fromTransactionalFunctionType(type);

        return resolveWeight(analysis, functionType, complexity);
    }

    public int calculateDataFunctionWeight(DataFunctionType type,
                                           FunctionPointComplexity complexity) {
        FunctionPointFunctionType functionType =
                FunctionPointFunctionType.fromDataFunctionType(type);

        return getDefaultWeight(functionType, complexity);
    }

    public int calculateTransactionalFunctionWeight(TransactionalFunctionType type,
                                                    FunctionPointComplexity complexity) {
        FunctionPointFunctionType functionType =
                FunctionPointFunctionType.fromTransactionalFunctionType(type);

        return getDefaultWeight(functionType, complexity);
    }

    private FunctionPointAnalysisSummary buildSummary(FunctionPointAnalysis analysis,
                                                      List<DataFunction> dataFunctions,
                                                      List<TransactionalFunction> transactionalFunctions,
                                                      Integer totalDegreeOfInfluence,
                                                      Double valueAdjustmentFactor) {
        int unadjustedFunctionPoints =
                calculateUnadjustedFunctionPoints(analysis, dataFunctions, transactionalFunctions);

        int tdi = totalDegreeOfInfluence != null ? totalDegreeOfInfluence : 0;
        double vaf = valueAdjustmentFactor != null ? valueAdjustmentFactor : 0.65 + (0.01 * tdi);
        double adjustedFunctionPoints = unadjustedFunctionPoints * vaf;

        List<Map<String, Object>> breakdownRows =
                buildBreakdownRows(analysis, dataFunctions, transactionalFunctions);

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

    private int calculateUnadjustedFunctionPoints(FunctionPointAnalysis analysis,
                                                  List<DataFunction> dataFunctions,
                                                  List<TransactionalFunction> transactionalFunctions) {
        int unadjustedFunctionPoints = 0;

        for (DataFunction dataFunction : dataFunctions) {
            unadjustedFunctionPoints += calculateDataFunctionWeight(
                    analysis,
                    dataFunction.getType(),
                    dataFunction.getComplexity()
            );
        }

        for (TransactionalFunction transactionalFunction : transactionalFunctions) {
            unadjustedFunctionPoints += calculateTransactionalFunctionWeight(
                    analysis,
                    transactionalFunction.getType(),
                    transactionalFunction.getComplexity()
            );
        }

        return unadjustedFunctionPoints;
    }

    private List<Map<String, Object>> buildBreakdownRows(FunctionPointAnalysis analysis,
                                                         List<DataFunction> dataFunctions,
                                                         List<TransactionalFunction> transactionalFunctions) {
        List<Map<String, Object>> rows = new ArrayList<>();

        rows.add(buildTransactionalFunctionRow(
                analysis,
                transactionalFunctions,
                TransactionalFunctionType.EI,
                FunctionPointFunctionType.EI,
                "fp.results.type.ei"
        ));

        rows.add(buildTransactionalFunctionRow(
                analysis,
                transactionalFunctions,
                TransactionalFunctionType.EO,
                FunctionPointFunctionType.EO,
                "fp.results.type.eo"
        ));

        rows.add(buildTransactionalFunctionRow(
                analysis,
                transactionalFunctions,
                TransactionalFunctionType.EQ,
                FunctionPointFunctionType.EQ,
                "fp.results.type.eq"
        ));

        rows.add(buildDataFunctionRow(
                analysis,
                dataFunctions,
                DataFunctionType.ILF,
                FunctionPointFunctionType.ILF,
                "fp.results.type.ilf"
        ));

        rows.add(buildDataFunctionRow(
                analysis,
                dataFunctions,
                DataFunctionType.EIF,
                FunctionPointFunctionType.EIF,
                "fp.results.type.eif"
        ));

        return rows;
    }

    private Map<String, Object> buildDataFunctionRow(FunctionPointAnalysis analysis,
                                                     List<DataFunction> dataFunctions,
                                                     DataFunctionType type,
                                                     FunctionPointFunctionType functionType,
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

        int simpleWeight = resolveWeight(analysis, functionType, FunctionPointComplexity.LOW);
        int averageWeight = resolveWeight(analysis, functionType, FunctionPointComplexity.AVERAGE);
        int highWeight = resolveWeight(analysis, functionType, FunctionPointComplexity.HIGH);

        int totalCount = simpleCount + averageCount + highCount;
        int ufpContribution =
                simpleCount * simpleWeight +
                        averageCount * averageWeight +
                        highCount * highWeight;

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("labelKey", labelKey);
        row.put("simpleCount", simpleCount);
        row.put("averageCount", averageCount);
        row.put("highCount", highCount);
        row.put("totalCount", totalCount);
        row.put("ufpContribution", ufpContribution);

        return row;
    }

    private Map<String, Object> buildTransactionalFunctionRow(FunctionPointAnalysis analysis,
                                                              List<TransactionalFunction> transactionalFunctions,
                                                              TransactionalFunctionType type,
                                                              FunctionPointFunctionType functionType,
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

        int simpleWeight = resolveWeight(analysis, functionType, FunctionPointComplexity.LOW);
        int averageWeight = resolveWeight(analysis, functionType, FunctionPointComplexity.AVERAGE);
        int highWeight = resolveWeight(analysis, functionType, FunctionPointComplexity.HIGH);

        int totalCount = simpleCount + averageCount + highCount;
        int ufpContribution =
                simpleCount * simpleWeight +
                        averageCount * averageWeight +
                        highCount * highWeight;

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("labelKey", labelKey);
        row.put("simpleCount", simpleCount);
        row.put("averageCount", averageCount);
        row.put("highCount", highCount);
        row.put("totalCount", totalCount);
        row.put("ufpContribution", ufpContribution);

        return row;
    }

    private int resolveWeight(FunctionPointAnalysis analysis,
                              FunctionPointFunctionType functionType,
                              FunctionPointComplexity complexity) {
        if (functionType == null || complexity == null) {
            return 0;
        }

        if (analysis == null
                || analysis.getWeightMatrixEntries() == null
                || analysis.getWeightMatrixEntries().isEmpty()) {
            return getDefaultWeight(functionType, complexity);
        }

        return analysis.getWeightMatrixEntries()
                .stream()
                .filter(entry -> entry.getFunctionType() == functionType)
                .filter(entry -> entry.getComplexity() == complexity)
                .findFirst()
                .map(FunctionPointWeightMatrixEntry::getWeight)
                .map(this::normalizeWeight)
                .orElseGet(() -> getDefaultWeight(functionType, complexity));
    }

    private int getDefaultWeight(FunctionPointFunctionType functionType,
                                 FunctionPointComplexity complexity) {
        if (functionType == null || complexity == null) {
            return 0;
        }

        return functionType.getDefaultWeight(complexity);
    }

    private int normalizeWeight(Integer weight) {
        if (weight == null || weight < 0) {
            return 0;
        }

        return weight;
    }
}