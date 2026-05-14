package com.uniovi.estimacion.repositories.sizeanalyses.usecasepoints;

import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.usecases.UseCaseEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UseCaseEntryRepository extends JpaRepository<UseCaseEntry, Long> {

    @EntityGraph(attributePaths = {"useCasePointModule", "actors"})
    Page<UseCaseEntry> findByUseCasePointAnalysisEstimationProjectIdOrderByIdAsc(
            Long projectId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"useCasePointModule", "actors"})
    List<UseCaseEntry> findByUseCasePointAnalysisEstimationProjectIdOrderByIdAsc(
            Long projectId
    );

    @EntityGraph(attributePaths = {"useCasePointModule", "actors"})
    Page<UseCaseEntry> findByUseCasePointModuleIdOrderByIdAsc(
            Long moduleId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"useCasePointModule", "actors"})
    List<UseCaseEntry> findByUseCasePointModuleIdOrderByIdAsc(Long moduleId);

    boolean existsByActorsId(Long actorId);
}