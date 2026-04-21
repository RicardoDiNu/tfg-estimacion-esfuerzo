package com.uniovi.estimacion.repositories.functionpoints;

import com.uniovi.estimacion.entities.functionpoints.DataFunction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataFunctionRepository extends JpaRepository<DataFunction, Long> {

    @EntityGraph(attributePaths = "userRequirement")
    Page<DataFunction> findByFunctionPointAnalysisEstimationProjectIdOrderByIdAsc(Long projectId, Pageable pageable);

    @EntityGraph(attributePaths = "userRequirement")
    Page<DataFunction> findByUserRequirementIdOrderByIdAsc(Long requirementId, Pageable pageable);
}