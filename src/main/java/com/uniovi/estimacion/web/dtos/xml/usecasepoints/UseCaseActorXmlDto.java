package com.uniovi.estimacion.web.dtos.xml.usecasepoints;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JacksonXmlRootElement(localName = "actor")
@Getter
@Setter
@NoArgsConstructor
public class UseCaseActorXmlDto {

    @JacksonXmlProperty(isAttribute = true, localName = "ref")
    private String ref;

    @JacksonXmlProperty(isAttribute = true, localName = "complexity")
    private String complexity;

    private String name;
    private String description;
}
