package com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.factors;

public enum EnvironmentalFactorType {

    PROCESS_FAMILIARITY("ucp.environmentalFactor.processFamiliarity", 1.5),
    APPLICATION_EXPERIENCE("ucp.environmentalFactor.applicationExperience", 0.5),
    OBJECT_ORIENTED_EXPERIENCE("ucp.environmentalFactor.objectOrientedExperience", 1.0),
    LEAD_ANALYST_CAPABILITY("ucp.environmentalFactor.leadAnalystCapability", 0.5),
    MOTIVATION("ucp.environmentalFactor.motivation", 1.0),
    STABLE_REQUIREMENTS("ucp.environmentalFactor.stableRequirements", 2.0),
    PART_TIME_WORKERS("ucp.environmentalFactor.partTimeWorkers", -1.0),
    DIFFICULT_LANGUAGE("ucp.environmentalFactor.difficultLanguage", -1.0);

    private final String messageKey;
    private final double weight;

    EnvironmentalFactorType(String messageKey, double weight) {
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