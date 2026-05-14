package com.uniovi.estimacion.repositories.effortconversions.delphi;

import com.uniovi.estimacion.entities.effortconversions.delphi.DelphiEstimation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DelphiEstimationRepository extends JpaRepository<DelphiEstimation, Long> {

    Optional<DelphiEstimation> findFirstBySourceAnalysisIdAndSourceTechniqueCodeAndActiveTrueOrderByCreatedAtDesc(
            Long sourceAnalysisId,
            String sourceTechniqueCode
    );

    List<DelphiEstimation> findBySourceAnalysisIdAndSourceTechniqueCodeOrderByCreatedAtDesc(
            Long sourceAnalysisId,
            String sourceTechniqueCode
    );

    List<DelphiEstimation> findByEstimationProjectIdOrderByCreatedAtDesc(Long projectId);

    Optional<DelphiEstimation> findByIdAndEstimationProjectId(Long id, Long estimationProjectId);

    List<DelphiEstimation> findAllBySourceAnalysisIdAndSourceTechniqueCode(Long id, String techniqueCode);

    void deleteByEstimationProjectId(Long projectId);
}