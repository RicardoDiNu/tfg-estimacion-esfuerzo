package com.uniovi.estimacion.repositories.sizeanalyses.usecasepoints;

import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.modules.UseCasePointModule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UseCasePointModuleRepository extends JpaRepository<UseCasePointModule, Long> {

    Page<UseCasePointModule> findByUseCasePointAnalysisEstimationProjectIdOrderByIdAsc(
            Long projectId,
            Pageable pageable
    );

    List<UseCasePointModule> findByUseCasePointAnalysisEstimationProjectIdOrderByIdAsc(
            Long projectId
    );

    @EntityGraph(attributePaths = "useCases")
    Optional<UseCasePointModule> findByIdAndUseCasePointAnalysisEstimationProjectId(
            Long moduleId,
            Long projectId
    );
}