package com.uniovi.estimacion.web.dtos.xml.usecasepoints;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@JacksonXmlRootElement(localName = "useCasePointAnalysisExport")
@Getter
@Setter
@NoArgsConstructor
public class UseCasePointAnalysisXmlDto {

    @JacksonXmlProperty(isAttribute = true)
    private String version = "1.0";

    private String systemBoundaryDescription;

    @JacksonXmlElementWrapper(localName = "actors")
    @JacksonXmlProperty(localName = "actor")
    private List<UseCaseActorXmlDto> actors;

    @JacksonXmlElementWrapper(localName = "modules")
    @JacksonXmlProperty(localName = "module")
    private List<UseCasePointModuleXmlDto> modules;

    @JacksonXmlElementWrapper(localName = "useCases")
    @JacksonXmlProperty(localName = "useCase")
    private List<UseCaseEntryXmlDto> useCases;

    @JacksonXmlElementWrapper(localName = "technicalFactors")
    @JacksonXmlProperty(localName = "technicalFactor")
    private List<UseCaseTechnicalFactorXmlDto> technicalFactors;

    @JacksonXmlElementWrapper(localName = "environmentalFactors")
    @JacksonXmlProperty(localName = "environmentalFactor")
    private List<UseCaseEnvironmentalFactorXmlDto> environmentalFactors;
}
