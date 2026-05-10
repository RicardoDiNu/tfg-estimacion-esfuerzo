package com.uniovi.estimacion.web.dtos.xml.functionpoints;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FunctionPointModuleXmlDto {

    @JacksonXmlProperty(isAttribute = true, localName = "ref")
    private String ref;

    private String name;

    private String description;
}
