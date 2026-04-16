package com.uniovi.estimacion.repositories;

import com.uniovi.estimacion.entities.EstimationProject;
import org.springframework.data.repository.CrudRepository;

public interface EstimationProjectRepository extends CrudRepository<EstimationProject, Long> {
}