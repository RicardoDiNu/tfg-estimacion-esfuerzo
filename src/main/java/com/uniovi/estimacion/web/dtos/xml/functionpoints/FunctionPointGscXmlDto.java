package com.uniovi.estimacion.web.dtos.xml.functionpoints;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class FunctionPointGscXmlDto {

    @JacksonXmlProperty(isAttribute = true, localName = "type")
    private String type;

    @JacksonXmlProperty(isAttribute = true, localName = "degreeOfInfluence")
    private Integer degreeOfInfluence;

    @JacksonXmlProperty(localName = "customText")
    private String customText;
}