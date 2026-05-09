package com.uniovi.estimacion.web.forms.projects;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectMembershipForm {

    private Long workerId;

    private Boolean canEditEstimationData = false;

    private Boolean canManageEffortConversions = false;
}