package com.uniovi.estimacion.entities.sizeanalyses.functionpoints.weights;

import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.DataFunctionType;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.FunctionPointComplexity;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.TransactionalFunctionType;

public enum FunctionPointFunctionType {

    EI("fp.results.type.ei", 1, 3, 4, 6),
    EO("fp.results.type.eo", 2, 4, 5, 7),
    EQ("fp.results.type.eq", 3, 3, 4, 6),
    ILF("fp.results.type.ilf", 4, 7, 10, 15),
    EIF("fp.results.type.eif", 5, 5, 7, 10);

    private final String labelKey;
    private final int displayOrder;
    private final int defaultLowWeight;
    private final int defaultAverageWeight;
    private final int defaultHighWeight;

    FunctionPointFunctionType(String labelKey,
                              int displayOrder,
                              int defaultLowWeight,
                              int defaultAverageWeight,
                              int defaultHighWeight) {
        this.labelKey = labelKey;
        this.displayOrder = displayOrder;
        this.defaultLowWeight = defaultLowWeight;
        this.defaultAverageWeight = defaultAverageWeight;
        this.defaultHighWeight = defaultHighWeight;
    }

    public String getLabelKey() {
        return labelKey;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public int getDefaultWeight(FunctionPointComplexity complexity) {
        if (complexity == null) {
            return 0;
        }

        return switch (complexity) {
            case LOW -> defaultLowWeight;
            case AVERAGE -> defaultAverageWeight;
            case HIGH -> defaultHighWeight;
        };
    }

    public static FunctionPointFunctionType fromDataFunctionType(DataFunctionType type) {
        if (type == null) {
            return null;
        }

        return switch (type) {
            case ILF -> ILF;
            case EIF -> EIF;
        };
    }

    public static FunctionPointFunctionType fromTransactionalFunctionType(TransactionalFunctionType type) {
        if (type == null) {
            return null;
        }

        return switch (type) {
            case EI -> EI;
            case EO -> EO;
            case EQ -> EQ;
        };
    }
}