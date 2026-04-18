package com.uniovi.estimacion.services.functionpoints;

import com.uniovi.estimacion.entities.functionpoints.*;
import org.springframework.stereotype.Service;

@Service
public class FunctionPointCalculationService {

    public void recalculateAnalysis(FunctionPointAnalysis analysis) {
        int unadjustedFunctionPoints = 0;

        for (DataFunction dataFunction : analysis.getDataFunctions()) {
            FunctionPointComplexity complexity =
                    calculateDataFunctionComplexity(dataFunction.getDetCount(), dataFunction.getRetCount());
            int weight = calculateDataFunctionWeight(dataFunction.getType(), complexity);

            dataFunction.setComplexity(complexity);
            dataFunction.setWeight(weight);

            unadjustedFunctionPoints += weight;
        }

        for (TransactionalFunction transactionalFunction : analysis.getTransactionalFunctions()) {
            FunctionPointComplexity complexity =
                    calculateTransactionalFunctionComplexity(
                            transactionalFunction.getType(),
                            transactionalFunction.getDetCount(),
                            transactionalFunction.getFtrCount()
                    );
            int weight = calculateTransactionalFunctionWeight(transactionalFunction.getType(), complexity);

            transactionalFunction.setComplexity(complexity);
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

    public FunctionPointComplexity calculateDataFunctionComplexity(int detCount, int retCount) {
        if (retCount <= 1) {
            if (detCount <= 50) {
                return FunctionPointComplexity.LOW;
            }
            return FunctionPointComplexity.AVERAGE;
        }

        if (retCount <= 5) {
            if (detCount <= 19) {
                return FunctionPointComplexity.LOW;
            }
            if (detCount <= 50) {
                return FunctionPointComplexity.AVERAGE;
            }
            return FunctionPointComplexity.HIGH;
        }

        if (detCount <= 19) {
            return FunctionPointComplexity.AVERAGE;
        }
        return FunctionPointComplexity.HIGH;
    }

    public int calculateDataFunctionWeight(DataFunctionType type, FunctionPointComplexity complexity) {
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

    public FunctionPointComplexity calculateTransactionalFunctionComplexity(
            TransactionalFunctionType type,
            int detCount,
            int ftrCount
    ) {
        return switch (type) {
            case EI -> calculateEiComplexity(detCount, ftrCount);
            case EO, EQ -> calculateEoEqComplexity(detCount, ftrCount);
        };
    }

    private FunctionPointComplexity calculateEiComplexity(int detCount, int ftrCount) {
        if (ftrCount <= 1) {
            if (detCount <= 15) {
                return FunctionPointComplexity.LOW;
            }
            return FunctionPointComplexity.AVERAGE;
        }

        if (ftrCount == 2) {
            if (detCount <= 4) {
                return FunctionPointComplexity.LOW;
            }
            if (detCount <= 15) {
                return FunctionPointComplexity.AVERAGE;
            }
            return FunctionPointComplexity.HIGH;
        }

        if (detCount <= 4) {
            return FunctionPointComplexity.AVERAGE;
        }
        return FunctionPointComplexity.HIGH;
    }

    private FunctionPointComplexity calculateEoEqComplexity(int detCount, int ftrCount) {
        if (ftrCount <= 1) {
            if (detCount <= 5) {
                return FunctionPointComplexity.LOW;
            }
            if (detCount <= 19) {
                return FunctionPointComplexity.LOW;
            }
            return FunctionPointComplexity.AVERAGE;
        }

        if (ftrCount <= 3) {
            if (detCount <= 5) {
                return FunctionPointComplexity.LOW;
            }
            if (detCount <= 19) {
                return FunctionPointComplexity.AVERAGE;
            }
            return FunctionPointComplexity.HIGH;
        }

        if (detCount <= 5) {
            return FunctionPointComplexity.AVERAGE;
        }
        return FunctionPointComplexity.HIGH;
    }

    public int calculateTransactionalFunctionWeight(
            TransactionalFunctionType type,
            FunctionPointComplexity complexity
    ) {
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