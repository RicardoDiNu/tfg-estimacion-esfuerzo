package com.uniovi.estimacion.web.dtos.xml.functionpoints;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FunctionPointTransactionalFunctionXmlDto {

    @JacksonXmlProperty(isAttribute = true, localName = "requirementRef")
    private String requirementRef;

    private String name;

    private String description;

    private String type;

    private String complexity;
}
