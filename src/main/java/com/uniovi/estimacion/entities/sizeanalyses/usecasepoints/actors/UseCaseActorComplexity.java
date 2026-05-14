package com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.actors;

public enum UseCaseActorComplexity {

    SIMPLE("ucp.actor.complexity.simple", 1),
    AVERAGE("ucp.actor.complexity.average", 2),
    COMPLEX("ucp.actor.complexity.complex", 3);

    private final String messageKey;
    private final int weight;

    UseCaseActorComplexity(String messageKey, int weight) {
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