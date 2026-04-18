package com.uniovi.estimacion.repositories;

import com.uniovi.estimacion.entities.requirements.UserRequirement;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface UserRequirementRepository extends CrudRepository<UserRequirement, Long> {

    List<UserRequirement> findByEstimationProjectIdOrderByIdAsc(Long estimationProjectId);

    Optional<UserRequirement> findByIdAndEstimationProjectId(Long id, Long estimationProjectId);
}