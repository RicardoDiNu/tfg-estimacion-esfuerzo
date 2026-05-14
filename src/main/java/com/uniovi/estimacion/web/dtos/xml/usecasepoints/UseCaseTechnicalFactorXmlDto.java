package com.uniovi.estimacion.web.dtos.xml.usecasepoints;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JacksonXmlRootElement(localName = "technicalFactor")
@Getter
@Setter
@NoArgsConstructor
public class UseCaseTechnicalFactorXmlDto {

    @JacksonXmlProperty(isAttribute = true, localName = "type")
    private String type;

    @JacksonXmlProperty(isAttribute = true, localName = "degreeOfInfluence")
    private Integer degreeOfInfluence;
}
