package com.uniovi.estimacion.repositories.sizeanalyses.functionpoints;

import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.FunctionPointAnalysis;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface FunctionPointAnalysisRepository extends CrudRepository<FunctionPointAnalysis, Long> {

    Optional<FunctionPointAnalysis> findByEstimationProjectId(Long estimationProjectId);
}