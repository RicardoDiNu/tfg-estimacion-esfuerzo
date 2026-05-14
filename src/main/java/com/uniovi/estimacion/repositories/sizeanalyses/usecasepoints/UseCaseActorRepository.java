package com.uniovi.estimacion.repositories.sizeanalyses.usecasepoints;

import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.actors.UseCaseActor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UseCaseActorRepository extends JpaRepository<UseCaseActor, Long> {

    Page<UseCaseActor> findByUseCasePointAnalysisEstimationProjectIdOrderByIdAsc(Long projectId, Pageable pageable);

    List<UseCaseActor> findByUseCasePointAnalysisEstimationProjectIdOrderByIdAsc(Long projectId);
}