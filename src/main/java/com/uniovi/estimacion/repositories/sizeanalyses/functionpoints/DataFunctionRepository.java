package com.uniovi.estimacion.repositories.sizeanalyses.functionpoints;

import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.DataFunction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DataFunctionRepository extends JpaRepository<DataFunction, Long> {

    @EntityGraph(attributePaths = {
            "userRequirement",
            "userRequirement.functionPointModule"
    })
    Page<DataFunction> findByFunctionPointAnalysisEstimationProjectIdOrderByIdAsc(
            Long projectId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "userRequirement",
            "userRequirement.functionPointModule"
    })
    Page<DataFunction> findByUserRequirementIdOrderByIdAsc(
            Long requirementId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "userRequirement",
            "userRequirement.functionPointModule"
    })
    Page<DataFunction> findByUserRequirementFunctionPointModuleIdOrderByIdAsc(
            Long moduleId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "userRequirement",
            "userRequirement.functionPointModule"
    })
    List<DataFunction> findByUserRequirementFunctionPointModuleIdOrderByIdAsc(
            Long moduleId
    );
}