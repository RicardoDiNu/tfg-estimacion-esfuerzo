package com.uniovi.estimacion.web.dtos.xml.usecasepoints;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JacksonXmlRootElement(localName = "module")
@Getter
@Setter
@NoArgsConstructor
public class UseCasePointModuleXmlDto {

    @JacksonXmlProperty(isAttribute = true, localName = "ref")
    private String ref;

    private String name;
    private String description;
}
