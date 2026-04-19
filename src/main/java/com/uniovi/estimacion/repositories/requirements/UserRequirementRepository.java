package com.uniovi.estimacion.repositories.requirements;

import com.uniovi.estimacion.entities.requirements.UserRequirement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRequirementRepository extends JpaRepository<UserRequirement, Long> {

    List<UserRequirement> findByEstimationProjectIdOrderByIdAsc(Long projectId);

    Page<UserRequirement> findByEstimationProjectIdOrderByIdAsc(Long projectId, Pageable pageable);

    Optional<UserRequirement> findByIdAndEstimationProjectId(Long id, Long projectId);
}