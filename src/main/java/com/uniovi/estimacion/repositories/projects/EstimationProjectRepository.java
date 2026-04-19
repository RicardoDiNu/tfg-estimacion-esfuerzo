package com.uniovi.estimacion.repositories.projects;

import com.uniovi.estimacion.entities.projects.EstimationProject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstimationProjectRepository extends JpaRepository<EstimationProject, Long> {

    Page<EstimationProject> findAllByOrderByIdAsc(Pageable pageable);
}