package com.uniovi.estimacion.entities.effortconversions;

import com.uniovi.estimacion.entities.projects.EstimationProject;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "delphi_estimations")
@Getter
@Setter
@NoArgsConstructor
public class DelphiEstimation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "estimation_project_id", nullable = false)
    private EstimationProject estimationProject;

    @Column(nullable = false)
    private Long sourceAnalysisId;

    @Column(nullable = false, length = 50)
    private String sourceTechniqueCode;

    @Column(nullable = false, length = 30)
    private String sourceSizeUnitCode;

    @Column(nullable = false)
    private Double sourceProjectSizeSnapshot;

    @Column(nullable = false)
    private Long minimumModuleId;

    @Column(nullable = false, length = 150)
    private String minimumModuleNameSnapshot;

    @Column(nullable = false)
    private Double minimumModuleSizeSnapshot;

    @Column(nullable = false)
    private Long maximumModuleId;

    @Column(nullable = false, length = 150)
    private String maximumModuleNameSnapshot;

    @Column(nullable = false)
    private Double maximumModuleSizeSnapshot;

    @Column
    private Double minimumModuleEstimatedEffortHours;

    @Column
    private Double maximumModuleEstimatedEffortHours;

    @Column
    private Double regressionIntercept;

    @Column
    private Double regressionSlope;

    @Column(nullable = false)
    private Double confidencePercentage = 95.0;

    @Column(nullable = false)
    private Double acceptableDeviationPercentage = 10.0;

    @Column(nullable = false)
    private Integer maximumIterations = 2;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private Boolean outdated = false;

    @OneToMany(mappedBy = "delphiEstimation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DelphiIteration> iterations = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public void addIteration(DelphiIteration iteration) {
        iteration.setDelphiEstimation(this);
        this.iterations.add(iteration);
    }

    public void removeIteration(DelphiIteration iteration) {
        this.iterations.remove(iteration);
        iteration.setDelphiEstimation(null);
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