package com.uniovi.estimacion.web.dtos.xml.functionpoints;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@JacksonXmlRootElement(localName = "functionPointAnalysisExport")
@Getter
@Setter
@NoArgsConstructor
public class FunctionPointAnalysisXmlDto {

    @JacksonXmlProperty(isAttribute = true)
    private String version = "1.0";

    private String systemBoundaryDescription;

    @JacksonXmlElementWrapper(localName = "weightMatrix")
    @JacksonXmlProperty(localName = "entry")
    private List<FunctionPointWeightMatrixEntryXmlDto> weightMatrix;

    @JacksonXmlElementWrapper(localName = "gscs")
    @JacksonXmlProperty(localName = "gsc")
    private List<FunctionPointGscXmlDto> gscs;

    @JacksonXmlElementWrapper(localName = "modules")
    @JacksonXmlProperty(localName = "module")
    private List<FunctionPointModuleXmlDto> modules;

    @JacksonXmlElementWrapper(localName = "requirements")
    @JacksonXmlProperty(localName = "requirement")
    private List<FunctionPointRequirementXmlDto> requirements;

    @JacksonXmlElementWrapper(localName = "dataFunctions")
    @JacksonXmlProperty(localName = "dataFunction")
    private List<FunctionPointDataFunctionXmlDto> dataFunctions;

    @JacksonXmlElementWrapper(localName = "transactionalFunctions")
    @JacksonXmlProperty(localName = "transactionalFunction")
    private List<FunctionPointTransactionalFunctionXmlDto> transactionalFunctions;
}
