package com.uniovi.estimacion.web.dtos.xml.usecasepoints;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@JacksonXmlRootElement(localName = "useCase")
@Getter
@Setter
@NoArgsConstructor
public class UseCaseEntryXmlDto {

    @JacksonXmlProperty(isAttribute = true, localName = "ref")
    private String ref;

    @JacksonXmlProperty(isAttribute = true, localName = "moduleRef")
    private String moduleRef;

    @JacksonXmlProperty(isAttribute = true, localName = "transactionCount")
    private Integer transactionCount;

    private String name;
    private String description;
    private String triggerCondition;
    private String preconditions;
    private String postconditions;
    private String normalFlow;
    private String alternativeFlows;
    private String exceptionFlows;

    @JacksonXmlElementWrapper(localName = "actorRefs")
    @JacksonXmlProperty(localName = "actorRef")
    private List<String> actorRefs;
}
