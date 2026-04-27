package com.uniovi.estimacion.entities.analysis;

import com.uniovi.estimacion.entities.projects.EstimationProject;

public interface SizeAnalysis {

    Long getId();

    EstimationProject getEstimationProject();

    Double getCalculatedSizeValue();

    String getSizeUnitCode();

    String getTechniqueCode();
}