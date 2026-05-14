package com.uniovi.estimacion.repositories.sizeanalyses.functionpoints;

import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.requirements.UserRequirement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRequirementRepository extends JpaRepository<UserRequirement, Long> {

    @EntityGraph(attributePaths = {"functionPointModule"})
    List<UserRequirement> findByFunctionPointModuleFunctionPointAnalysisEstimationProjectIdOrderByIdAsc(
            Long projectId
    );

    @EntityGraph(attributePaths = {"functionPointModule"})
    Page<UserRequirement> findByFunctionPointModuleFunctionPointAnalysisEstimationProjectIdOrderByIdAsc(
            Long projectId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"functionPointModule"})
    List<UserRequirement> findByFunctionPointModuleIdOrderByIdAsc(Long moduleId);

    @EntityGraph(attributePaths = {"functionPointModule"})
    Page<UserRequirement> findByFunctionPointModuleIdOrderByIdAsc(Long moduleId,
                                                                  Pageable pageable);

    @EntityGraph(attributePaths = {"functionPointModule"})
    Optional<UserRequirement> findByIdAndFunctionPointModuleFunctionPointAnalysisEstimationProjectId(
            Long requirementId,
            Long projectId
    );

    @EntityGraph(attributePaths = {"functionPointModule"})
    Optional<UserRequirement> findByIdAndFunctionPointModuleId(Long requirementId,
                                                               Long moduleId);

    @EntityGraph(attributePaths = {"functionPointModule"})
    Optional<UserRequirement> findByIdAndFunctionPointModuleIdAndFunctionPointModuleFunctionPointAnalysisEstimationProjectId(
            Long requirementId,
            Long moduleId,
            Long projectId
    );
}