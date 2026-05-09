package com.uniovi.estimacion.repositories.sizeanalyses.functionpoints;

import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.modules.EstimationModule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EstimationModuleRepository extends JpaRepository<EstimationModule, Long> {

    List<EstimationModule> findByEstimationProjectIdOrderByDisplayOrderAscIdAsc(Long projectId);

    Optional<EstimationModule> findByIdAndEstimationProjectId(Long moduleId, Long projectId);

    long countByEstimationProjectId(Long projectId);

    List<EstimationModule> findByEstimationProjectIdOrderByIdAsc(Long projectId);
    Page<EstimationModule> findByEstimationProjectIdOrderByIdAsc(Long projectId, Pageable pageable);

}