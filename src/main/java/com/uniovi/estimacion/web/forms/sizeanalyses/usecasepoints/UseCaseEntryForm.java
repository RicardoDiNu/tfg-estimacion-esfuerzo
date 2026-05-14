package com.uniovi.estimacion.web.forms.sizeanalyses.usecasepoints;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class UseCaseEntryForm {

    private String name;

    private String description;

    private List<Long> actorIds = new ArrayList<>();

    private String triggerCondition;

    private String preconditions;

    private String postconditions;

    private String normalFlow;

    private String alternativeFlows;

    private String exceptionFlows;

    private Integer transactionCount;
}