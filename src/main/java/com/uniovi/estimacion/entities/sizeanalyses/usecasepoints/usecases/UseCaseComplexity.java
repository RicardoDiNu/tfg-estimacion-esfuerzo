package com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.usecases;

public enum UseCaseComplexity {

    SIMPLE("ucp.useCase.complexity.simple", 5),
    AVERAGE("ucp.useCase.complexity.average", 10),
    COMPLEX("ucp.useCase.complexity.complex", 15);

    private final String messageKey;
    private final int weight;

    UseCaseComplexity(String messageKey, int weight) {
        this.messageKey = messageKey;
        this.weight = weight;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public int getWeight() {
        return weight;
    }
}