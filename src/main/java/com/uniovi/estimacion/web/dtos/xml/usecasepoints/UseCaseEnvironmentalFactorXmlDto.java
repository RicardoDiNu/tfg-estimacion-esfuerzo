package com.uniovi.estimacion.web.dtos.xml.usecasepoints;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JacksonXmlRootElement(localName = "environmentalFactor")
@Getter
@Setter
@NoArgsConstructor
public class UseCaseEnvironmentalFactorXmlDto {

    @JacksonXmlProperty(isAttribute = true, localName = "type")
    private String type;

    @JacksonXmlProperty(isAttribute = true, localName = "degreeOfInfluence")
    private Integer degreeOfInfluence;
}
