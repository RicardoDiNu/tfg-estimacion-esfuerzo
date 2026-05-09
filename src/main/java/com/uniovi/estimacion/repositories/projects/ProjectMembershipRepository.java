package com.uniovi.estimacion.repositories.projects;

import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.projects.ProjectMembership;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectMembershipRepository extends JpaRepository<ProjectMembership, Long> {

    @EntityGraph(attributePaths = {"project", "worker"})
    List<ProjectMembership> findByProjectIdOrderByWorkerUsernameAsc(Long projectId);

    @EntityGraph(attributePaths = {"project", "worker"})
    Optional<ProjectMembership> findByProjectIdAndWorkerId(Long projectId, Long workerId);

    @EntityGraph(attributePaths = {"project", "worker"})
    Optional<ProjectMembership> findByProjectIdAndWorkerUsername(Long projectId, String username);

    boolean existsByProjectIdAndWorkerId(Long projectId, Long workerId);

    boolean existsByProjectIdAndWorkerUsername(Long projectId, String username);

    void deleteByProjectIdAndWorkerId(Long projectId, Long workerId);

    @EntityGraph(attributePaths = {"project", "worker"})
    Page<ProjectMembership> findByWorkerUsernameOrderByProjectIdAsc(String username, Pageable pageable);

    @Query("""
            select membership.project
            from ProjectMembership membership
            where membership.worker.username = :username
            order by membership.project.id asc
            """)
    Page<EstimationProject> findProjectsByWorkerUsername(@Param("username") String username,
                                                         Pageable pageable);
}