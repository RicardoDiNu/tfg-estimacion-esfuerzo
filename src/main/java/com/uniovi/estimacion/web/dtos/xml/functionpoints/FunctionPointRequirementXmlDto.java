package com.uniovi.estimacion.web.dtos.xml.functionpoints;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FunctionPointRequirementXmlDto {

    @JacksonXmlProperty(isAttribute = true, localName = "ref")
    private String ref;

    @JacksonXmlProperty(isAttribute = true, localName = "moduleRef")
    private String moduleRef;

    private String identifier;

    private String statement;
}
