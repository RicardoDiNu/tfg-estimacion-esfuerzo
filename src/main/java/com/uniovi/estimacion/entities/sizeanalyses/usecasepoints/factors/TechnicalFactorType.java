package com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.factors;

public enum TechnicalFactorType {

    DISTRIBUTED_SYSTEM("ucp.technicalFactor.distributedSystem", 2.0),
    RESPONSE_TIME("ucp.technicalFactor.responseTime", 1.0),
    END_USER_EFFICIENCY("ucp.technicalFactor.endUserEfficiency", 1.0),
    COMPLEX_PROCESSING("ucp.technicalFactor.complexProcessing", 1.0),
    REUSABLE_CODE("ucp.technicalFactor.reusableCode", 1.0),
    EASY_TO_INSTALL("ucp.technicalFactor.easyToInstall", 0.5),
    EASY_TO_USE("ucp.technicalFactor.easyToUse", 0.5),
    PORTABLE("ucp.technicalFactor.portable", 2.0),
    EASY_TO_CHANGE("ucp.technicalFactor.easyToChange", 1.0),
    CONCURRENT("ucp.technicalFactor.concurrent", 1.0),
    SECURITY("ucp.technicalFactor.security", 1.0),
    THIRD_PARTY_ACCESS("ucp.technicalFactor.thirdPartyAccess", 1.0),
    TRAINING("ucp.technicalFactor.training", 1.0);

    private final String messageKey;
    private final double weight;

    TechnicalFactorType(String messageKey, double weight) {
        this.messageKey = messageKey;
        this.weight = weight;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public double getWeight() {
        return weight;
    }
}