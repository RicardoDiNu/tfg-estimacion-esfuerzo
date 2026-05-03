package com.uniovi.estimacion.entities.effortconversions.transformationfunctions;

import com.uniovi.estimacion.common.codes.EffortConversionTechniqueCodes;
import com.uniovi.estimacion.entities.effortconversions.AbstractEffortConversion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "transformation_function_conversions")
@Getter
@Setter
@NoArgsConstructor
public class TransformationFunctionConversion extends AbstractEffortConversion {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "transformation_function_id", nullable = false)
    private TransformationFunction transformationFunction;

    @Column(nullable = false, length = 120)
    private String functionNameSnapshot;

    @Column(nullable = false)
    private Double interceptSnapshot;

    @Column(nullable = false)
    private Double slopeSnapshot;

    @Override
    public String getConversionTechniqueCode() {
        return EffortConversionTechniqueCodes.TRANSFORMATION_FUNCTION;
    }

    @Override
    public boolean isFinished() {
        return transformationFunction != null
                && interceptSnapshot != null
                && slopeSnapshot != null;
    }
}