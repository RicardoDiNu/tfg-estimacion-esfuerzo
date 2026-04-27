package com.uniovi.estimacion.entities.functionpoints.gscs;

import lombok.Getter;

@Getter
public enum GeneralSystemCharacteristicType {
    DATA_COMMUNICATIONS(2, "fp.gsc.characteristic.2"),
    DISTRIBUTED_DATA_PROCESSING(3, "fp.gsc.characteristic.3"),
    PERFORMANCE(4, "fp.gsc.characteristic.4"),
    HEAVILY_USED_CONFIGURATION(5, "fp.gsc.characteristic.5"),
    TRANSACTION_RATE(9, "fp.gsc.characteristic.9"),
    ON_LINE_DATA_ENTRY(6, "fp.gsc.characteristic.6"),
    END_USER_EFFICIENCY(14, "fp.gsc.characteristic.14"),
    ON_LINE_UPDATE(8, "fp.gsc.characteristic.8"),
    COMPLEX_PROCESSING(10, "fp.gsc.characteristic.10"),
    REUSABILITY(11, "fp.gsc.characteristic.11"),
    INSTALLATION_EASE(12, "fp.gsc.characteristic.12"),
    OPERATIONAL_EASE(1, "fp.gsc.characteristic.1"),
    MULTIPLE_SITES(13, "fp.gsc.characteristic.13"),
    FACILITATE_CHANGE(7, "fp.gsc.characteristic.7");

    private final int order;
    private final String messageKey;

    GeneralSystemCharacteristicType(int order, String messageKey) {
        this.order = order;
        this.messageKey = messageKey;
    }

}