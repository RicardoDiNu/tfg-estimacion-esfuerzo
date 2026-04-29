package com.uniovi.estimacion.entities.effortconversions;

import com.uniovi.estimacion.entities.projects.EstimationProject;

public interface EffortConversion {

    Long getId();

    EstimationProject getEstimationProject();

    Long getSourceAnalysisId();

    String getSourceTechniqueCode();

    String getSourceSizeUnitCode();

    Double getSourceProjectSizeSnapshot();

    String getConversionTechniqueCode();

    boolean isFinished();
}