package com.uniovi.estimacion.entities.functionpoints;

import com.uniovi.estimacion.common.codes.SizeTechniqueCodes;
import com.uniovi.estimacion.common.codes.SizeUnitCodes;
import com.uniovi.estimacion.entities.analysis.AbstractSizeAnalysis;
import com.uniovi.estimacion.entities.functionpoints.functions.DataFunction;
import com.uniovi.estimacion.entities.functionpoints.functions.TransactionalFunction;
import com.uniovi.estimacion.entities.functionpoints.gscs.GeneralSystemCharacteristicAssessment;
import com.uniovi.estimacion.entities.projects.EstimationProject;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "function_point_analyses",
        uniqueConstraints = @UniqueConstraint(columnNames = "estimation_project_id")
)
@Getter
@Setter
@NoArgsConstructor
public class FunctionPointAnalysis extends AbstractSizeAnalysis {

    @Column(nullable = false, length = 2000)
    private String systemBoundaryDescription;

    @Column(nullable = false)
    private Integer unadjustedFunctionPoints = 0;

    @Column(nullable = false)
    private Integer totalDegreeOfInfluence = 0;

    @Column(nullable = false)
    private Double valueAdjustmentFactor = 0.65;

    @Column(nullable = false)
    private Double adjustedFunctionPoints = 0.0;

    @OneToMany(mappedBy = "functionPointAnalysis", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DataFunction> dataFunctions = new ArrayList<>();

    @OneToMany(mappedBy = "functionPointAnalysis", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransactionalFunction> transactionalFunctions = new ArrayList<>();

    @OneToMany(mappedBy = "functionPointAnalysis", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GeneralSystemCharacteristicAssessment> generalSystemCharacteristicAssessments = new ArrayList<>();

    public FunctionPointAnalysis(EstimationProject estimationProject, String systemBoundaryDescription) {
        setEstimationProject(estimationProject);
        this.systemBoundaryDescription = systemBoundaryDescription;
    }

    @Override
    public Double getCalculatedSizeValue() {
        return adjustedFunctionPoints;
    }

    @Override
    public String getSizeUnitCode() {
        return SizeUnitCodes.FP;
    }

    @Override
    public String getTechniqueCode() {
        return SizeTechniqueCodes.FUNCTION_POINTS;
    }
}