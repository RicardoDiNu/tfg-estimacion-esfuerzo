package com.uniovi.estimacion.entities.sizeanalyses.functionpoints.modules;

import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.requirements.UserRequirement;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "estimation_modules")
@Getter
@Setter
@NoArgsConstructor
public class EstimationModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "estimation_project_id", nullable = false)
    private EstimationProject estimationProject;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Integer displayOrder = 0;

    @OneToMany(mappedBy = "estimationModule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserRequirement> userRequirements = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public EstimationModule(EstimationProject estimationProject, String name, String description, Integer displayOrder) {
        this.estimationProject = estimationProject;
        this.name = name;
        this.description = description;
        this.displayOrder = displayOrder;
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