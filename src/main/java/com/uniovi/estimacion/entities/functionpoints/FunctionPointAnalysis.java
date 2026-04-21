package com.uniovi.estimacion.entities.functionpoints;

import com.uniovi.estimacion.entities.projects.EstimationProject;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
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
public class FunctionPointAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "estimation_project_id", nullable = false, unique = true)
    private EstimationProject estimationProject;

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

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public FunctionPointAnalysis(EstimationProject estimationProject, String systemBoundaryDescription) {
        this.estimationProject = estimationProject;
        this.systemBoundaryDescription = systemBoundaryDescription;
    }

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}