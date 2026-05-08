package com.uniovi.estimacion.entities.usecasepoints;

import com.uniovi.estimacion.common.codes.SizeTechniqueCodes;
import com.uniovi.estimacion.common.codes.SizeUnitCodes;
import com.uniovi.estimacion.entities.analysis.AbstractSizeAnalysis;
import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.usecasepoints.actors.UseCaseActor;
import com.uniovi.estimacion.entities.usecasepoints.factors.EnvironmentalFactorAssessment;
import com.uniovi.estimacion.entities.usecasepoints.factors.TechnicalFactorAssessment;
import com.uniovi.estimacion.entities.usecasepoints.usecases.UseCaseEntry;
import com.uniovi.estimacion.entities.usecasepoints.modules.UseCasePointModule;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "use_case_point_analyses",
        uniqueConstraints = @UniqueConstraint(columnNames = "estimation_project_id")
)
@Getter
@Setter
@NoArgsConstructor
public class UseCasePointAnalysis extends AbstractSizeAnalysis {

    @Column(nullable = false, length = 2000)
    private String systemBoundaryDescription;

    @Column(nullable = false)
    private Integer unadjustedActorWeight = 0;

    @Column(nullable = false)
    private Integer unadjustedUseCaseWeight = 0;

    @Column(nullable = false)
    private Integer unadjustedUseCasePoints = 0;

    @Column(nullable = false)
    private Double technicalFactor = 0.0;

    @Column(nullable = false)
    private Double technicalComplexityFactor = 0.6;

    @Column(nullable = false)
    private Double environmentalFactor = 0.0;

    @Column(nullable = false)
    private Double environmentalComplexityFactor = 1.4;

    @Column(nullable = false)
    private Double adjustedUseCasePoints = 0.0;

    @OneToMany(mappedBy = "useCasePointAnalysis", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UseCaseActor> actors = new ArrayList<>();

    @OneToMany(mappedBy = "useCasePointAnalysis", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UseCasePointModule> modules = new ArrayList<>();

    @OneToMany(mappedBy = "useCasePointAnalysis", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UseCaseEntry> useCases = new ArrayList<>();

    @OneToMany(mappedBy = "useCasePointAnalysis", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TechnicalFactorAssessment> technicalFactorAssessments = new ArrayList<>();

    @OneToMany(mappedBy = "useCasePointAnalysis", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EnvironmentalFactorAssessment> environmentalFactorAssessments = new ArrayList<>();


    public UseCasePointAnalysis(EstimationProject estimationProject, String systemBoundaryDescription) {
        setEstimationProject(estimationProject);
        this.systemBoundaryDescription = systemBoundaryDescription;
    }

    @Override
    public Double getCalculatedSizeValue() {
        return adjustedUseCasePoints;
    }

    @Override
    public String getSizeUnitCode() {
        return SizeUnitCodes.UCP;
    }

    @Override
    public String getTechniqueCode() {
        return SizeTechniqueCodes.USE_CASE_POINTS;
    }
}