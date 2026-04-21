package com.uniovi.estimacion.repositories.projects;

import com.uniovi.estimacion.entities.projects.EstimationProject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstimationProjectRepository extends JpaRepository<EstimationProject, Long> {

    Page<EstimationProject> findAllByOrderByIdAsc(Pageable pageable);

    Page<EstimationProject> findByOwnerUsernameOrderByIdAsc(String username, Pageable pageable);

    Optional<EstimationProject> findByIdAndOwnerUsername(Long id, String username);
}