package com.uniovi.estimacion.web.dtos.xml.functionpoints;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FunctionPointWeightMatrixEntryXmlDto {

    @JacksonXmlProperty(isAttribute = true, localName = "functionType")
    private String functionType;

    @JacksonXmlProperty(isAttribute = true, localName = "complexity")
    private String complexity;

    @JacksonXmlProperty(isAttribute = true, localName = "weight")
    private Integer weight;
}
