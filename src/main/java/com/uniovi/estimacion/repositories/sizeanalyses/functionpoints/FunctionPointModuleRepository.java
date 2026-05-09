package com.uniovi.estimacion.repositories.sizeanalyses.functionpoints;

import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.modules.FunctionPointModule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FunctionPointModuleRepository extends JpaRepository<FunctionPointModule, Long> {

    List<FunctionPointModule> findByFunctionPointAnalysisEstimationProjectIdOrderByDisplayOrderAscIdAsc(Long projectId);

    Page<FunctionPointModule> findByFunctionPointAnalysisEstimationProjectIdOrderByDisplayOrderAscIdAsc(
            Long projectId,
            Pageable pageable
    );

    Optional<FunctionPointModule> findByIdAndFunctionPointAnalysisEstimationProjectId(
            Long moduleId,
            Long projectId
    );

    long countByFunctionPointAnalysisEstimationProjectId(Long projectId);

}