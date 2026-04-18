package com.uniovi.estimacion.entities.functionpoints;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GeneralSystemCharacteristicType {

    DATA_COMMUNICATIONS("Data Communications"),
    DISTRIBUTED_DATA_PROCESSING("Distributed Data Processing"),
    PERFORMANCE("Performance"),
    HEAVILY_USED_CONFIGURATION("Heavily Used Configuration"),
    TRANSACTION_RATE("Transaction Rate"),
    ON_LINE_DATA_ENTRY("On-line Data Entry"),
    END_USER_EFFICIENCY("End-user Efficiency"),
    ON_LINE_UPDATE("On-line Update"),
    COMPLEX_PROCESSING("Complex Processing"),
    REUSABILITY("Reusability"),
    INSTALLATION_EASE("Installation Ease"),
    OPERATIONAL_EASE("Operational Ease"),
    MULTIPLE_SITES("Multiple Sites"),
    FACILITATE_CHANGE("Facilitate Change");

    private final String label;
}