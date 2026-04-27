package com.uniovi.estimacion.repositories.requirements;

import com.uniovi.estimacion.entities.requirements.UserRequirement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRequirementRepository extends JpaRepository<UserRequirement, Long> {

    List<UserRequirement> findByEstimationModuleEstimationProjectIdOrderByIdAsc(Long projectId);

    List<UserRequirement> findByEstimationModuleIdOrderByIdAsc(Long moduleId);

    Page<UserRequirement> findByEstimationModuleEstimationProjectIdOrderByIdAsc(Long projectId, Pageable pageable);

    Page<UserRequirement> findByEstimationModuleIdOrderByIdAsc(Long moduleId, Pageable pageable);

    Optional<UserRequirement> findByIdAndEstimationModuleEstimationProjectId(Long requirementId, Long projectId);

    Optional<UserRequirement> findByIdAndEstimationModuleId(Long requirementId, Long moduleId);

    Optional<UserRequirement> findByIdAndEstimationProjectId(Long requirementId, Long projectId);

    Iterable<? extends UserRequirement> findByEstimationProjectIdOrderByIdAsc(Long projectId);
}