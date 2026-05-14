package com.uniovi.estimacion.entities.effortconversions;

import com.uniovi.estimacion.entities.projects.EstimationProject;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
public abstract class AbstractEffortConversion implements EffortConversion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "estimation_project_id", nullable = false)
    protected EstimationProject estimationProject;

    @Column(nullable = false)
    protected Long sourceAnalysisId;

    @Column(nullable = false, length = 50)
    protected String sourceTechniqueCode;

    @Column(nullable = false, length = 30)
    protected String sourceSizeUnitCode;

    @Column(nullable = false)
    protected Double sourceProjectSizeSnapshot;

    @Column(nullable = false)
    protected Boolean active = true;

    @Column(nullable = false)
    protected Boolean outdated = false;

    @Column(nullable = false, updatable = false)
    protected LocalDateTime createdAt;

    @Column(nullable = false)
    protected LocalDateTime updatedAt;

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