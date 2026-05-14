package com.uniovi.estimacion.repositories.sizeanalyses.usecasepoints;

import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.factors.EnvironmentalFactorAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnvironmentalFactorAssessmentRepository extends JpaRepository<EnvironmentalFactorAssessment, Long> {

    List<EnvironmentalFactorAssessment> findByUseCasePointAnalysisEstimationProjectIdOrderByFactorTypeAsc(Long projectId);
}