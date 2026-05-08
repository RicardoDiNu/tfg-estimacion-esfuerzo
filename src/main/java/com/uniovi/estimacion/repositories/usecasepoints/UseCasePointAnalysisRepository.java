package com.uniovi.estimacion.repositories.usecasepoints;

import com.uniovi.estimacion.entities.usecasepoints.UseCasePointAnalysis;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface UseCasePointAnalysisRepository extends CrudRepository<UseCasePointAnalysis, Long> {

    Optional<UseCasePointAnalysis> findByEstimationProjectId(Long estimationProjectId);
}