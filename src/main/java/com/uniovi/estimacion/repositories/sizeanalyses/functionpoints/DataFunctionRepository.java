package com.uniovi.estimacion.repositories.sizeanalyses.functionpoints;

import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.DataFunction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DataFunctionRepository extends JpaRepository<DataFunction, Long> {

    @EntityGraph(attributePaths = "userRequirement")
    Page<DataFunction> findByFunctionPointAnalysisEstimationProjectIdOrderByIdAsc(Long projectId, Pageable pageable);

    @EntityGraph(attributePaths = "userRequirement")
    Page<DataFunction> findByUserRequirementIdOrderByIdAsc(Long requirementId, Pageable pageable);

    Page<DataFunction> findByUserRequirementEstimationModuleIdOrderByIdAsc(Long moduleId, Pageable pageable);

    List<DataFunction> findByUserRequirementEstimationModuleIdOrderByIdAsc(Long moduleId);
}