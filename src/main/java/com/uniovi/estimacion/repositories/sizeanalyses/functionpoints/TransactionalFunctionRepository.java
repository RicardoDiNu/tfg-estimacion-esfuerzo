package com.uniovi.estimacion.repositories.sizeanalyses.functionpoints;

import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.TransactionalFunction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionalFunctionRepository extends JpaRepository<TransactionalFunction, Long> {

    @EntityGraph(attributePaths = {
            "userRequirement",
            "userRequirement.functionPointModule"
    })
    Page<TransactionalFunction> findByFunctionPointAnalysisEstimationProjectIdOrderByIdAsc(
            Long projectId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "userRequirement",
            "userRequirement.functionPointModule"
    })
    Page<TransactionalFunction> findByUserRequirementIdOrderByIdAsc(
            Long requirementId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "userRequirement",
            "userRequirement.functionPointModule"
    })
    Page<TransactionalFunction> findByUserRequirementFunctionPointModuleIdOrderByIdAsc(
            Long moduleId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "userRequirement",
            "userRequirement.functionPointModule"
    })
    List<TransactionalFunction> findByUserRequirementFunctionPointModuleIdOrderByIdAsc(
            Long moduleId
    );
}