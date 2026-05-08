package com.uniovi.estimacion.web.forms.usecasepoints;

import com.uniovi.estimacion.entities.usecasepoints.actors.UseCaseActorComplexity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UseCaseActorForm {

    private String name;

    private String description;

    private UseCaseActorComplexity complexity;
}