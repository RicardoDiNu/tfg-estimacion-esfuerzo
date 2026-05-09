package com.uniovi.estimacion.repositories.sizeanalyses.usecasepoints;

import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.factors.TechnicalFactorAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TechnicalFactorAssessmentRepository extends JpaRepository<TechnicalFactorAssessment, Long> {

    List<TechnicalFactorAssessment> findByUseCasePointAnalysisEstimationProjectIdOrderByFactorTypeAsc(Long projectId);
}