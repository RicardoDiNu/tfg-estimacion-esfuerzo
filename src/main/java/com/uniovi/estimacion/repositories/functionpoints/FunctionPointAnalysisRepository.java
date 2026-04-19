package com.uniovi.estimacion.repositories.functionpoints;

import com.uniovi.estimacion.entities.functionpoints.FunctionPointAnalysis;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface FunctionPointAnalysisRepository extends CrudRepository<FunctionPointAnalysis, Long> {

    Optional<FunctionPointAnalysis> findByEstimationProjectId(Long estimationProjectId);
}