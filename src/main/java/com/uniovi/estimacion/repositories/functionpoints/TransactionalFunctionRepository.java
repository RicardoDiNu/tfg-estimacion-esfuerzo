package com.uniovi.estimacion.repositories.functionpoints;

import com.uniovi.estimacion.entities.functionpoints.TransactionalFunction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionalFunctionRepository extends JpaRepository<TransactionalFunction, Long> {

    @EntityGraph(attributePaths = "userRequirement")
    Page<TransactionalFunction> findByFunctionPointAnalysisEstimationProjectIdOrderByIdAsc(Long projectId, Pageable pageable);

    @EntityGraph(attributePaths = "userRequirement")
    Page<TransactionalFunction> findByUserRequirementIdOrderByIdAsc(Long requirementId, Pageable pageable);
}