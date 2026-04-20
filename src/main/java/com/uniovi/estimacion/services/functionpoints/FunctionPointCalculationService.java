package com.uniovi.estimacion.services.functionpoints;

import com.uniovi.estimacion.entities.functionpoints.DataFunction;
import com.uniovi.estimacion.entities.functionpoints.DataFunctionType;
import com.uniovi.estimacion.entities.functionpoints.FunctionPointAnalysis;
import com.uniovi.estimacion.entities.functionpoints.FunctionPointComplexity;
import com.uniovi.estimacion.entities.functionpoints.GeneralSystemCharacteristicAssessment;
import com.uniovi.estimacion.entities.functionpoints.TransactionalFunction;
import com.uniovi.estimacion.entities.functionpoints.TransactionalFunctionType;
import org.springframework.stereotype.Service;

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
}