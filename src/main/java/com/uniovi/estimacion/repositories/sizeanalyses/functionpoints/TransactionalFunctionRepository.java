package com.uniovi.estimacion.repositories.sizeanalyses.functionpoints;

import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.TransactionalFunction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionalFunctionRepository extends JpaRepository<TransactionalFunction, Long> {

    @EntityGraph(attributePaths = "userRequirement")
    Page<TransactionalFunction> findByFunctionPointAnalysisEstimationProjectIdOrderByIdAsc(Long projectId, Pageable pageable);

    @EntityGraph(attributePaths = "userRequirement")
    Page<TransactionalFunction> findByUserRequirementIdOrderByIdAsc(Long requirementId, Pageable pageable);

    Page<TransactionalFunction> findByUserRequirementEstimationModuleIdOrderByIdAsc(Long moduleId, Pageable pageable);

    List<TransactionalFunction> findByUserRequirementEstimationModuleIdOrderByIdAsc(Long moduleId);
}