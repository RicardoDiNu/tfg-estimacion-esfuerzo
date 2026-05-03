package com.uniovi.estimacion.repositories.effortconversions;

import com.uniovi.estimacion.entities.effortconversions.transformationfunctions.TransformationFunction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransformationFunctionRepository extends JpaRepository<TransformationFunction, Long> {

    List<TransformationFunction> findByActiveTrueAndSourceTechniqueCodeAndSourceSizeUnitCodeAndPredefinedTrueOrderByNameAsc(
            String sourceTechniqueCode,
            String sourceSizeUnitCode
    );

    List<TransformationFunction> findByActiveTrueAndSourceTechniqueCodeAndSourceSizeUnitCodeAndOwnerIdOrderByNameAsc(
            String sourceTechniqueCode,
            String sourceSizeUnitCode,
            Long ownerId
    );

    boolean existsByNameAndSourceTechniqueCodeAndSourceSizeUnitCodeAndPredefinedTrue(
            String name,
            String sourceTechniqueCode,
            String sourceSizeUnitCode
    );
}