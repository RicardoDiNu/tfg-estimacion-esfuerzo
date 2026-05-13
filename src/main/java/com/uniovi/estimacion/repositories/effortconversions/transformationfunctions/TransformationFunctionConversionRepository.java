package com.uniovi.estimacion.repositories.effortconversions.transformationfunctions;

import com.uniovi.estimacion.entities.effortconversions.transformationfunctions.TransformationFunctionConversion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransformationFunctionConversionRepository extends JpaRepository<TransformationFunctionConversion, Long> {

    Optional<TransformationFunctionConversion> findFirstBySourceAnalysisIdAndSourceTechniqueCodeAndActiveTrueOrderByCreatedAtDesc(
            Long sourceAnalysisId,
            String sourceTechniqueCode
    );

    List<TransformationFunctionConversion> findBySourceAnalysisIdAndSourceTechniqueCodeOrderByCreatedAtDesc(
            Long sourceAnalysisId,
            String sourceTechniqueCode
    );

    Optional<TransformationFunctionConversion> findByIdAndEstimationProjectId(
            Long id,
            Long estimationProjectId
    );

    void deleteByEstimationProjectId(Long projectId);
}