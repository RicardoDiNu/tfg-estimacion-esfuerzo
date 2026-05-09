package com.uniovi.estimacion.entities.sizeanalyses.functionpoints.modules;

import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.FunctionPointAnalysis;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.requirements.UserRequirement;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "function_point_modules")
@Getter
@Setter
@NoArgsConstructor
public class FunctionPointModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "function_point_analysis_id", nullable = false)
    private FunctionPointAnalysis functionPointAnalysis;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Integer displayOrder = 0;

    @OneToMany(mappedBy = "functionPointModule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserRequirement> userRequirements = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public FunctionPointModule(FunctionPointAnalysis functionPointAnalysis,
                               String name,
                               String description,
                               Integer displayOrder) {
        this.functionPointAnalysis = functionPointAnalysis;
        this.name = name;
        this.description = description;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
    }

    public void addUserRequirement(UserRequirement userRequirement) {
        userRequirement.setFunctionPointModule(this);
        this.userRequirements.add(userRequirement);
    }

    public void removeUserRequirement(UserRequirement userRequirement) {
        this.userRequirements.remove(userRequirement);
        userRequirement.setFunctionPointModule(null);
    }

    @Transient
    public EstimationProject getEstimationProject() {
        return functionPointAnalysis != null
                ? functionPointAnalysis.getEstimationProject()
                : null;
    }

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.displayOrder == null) {
            this.displayOrder = 0;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();

        if (this.displayOrder == null) {
            this.displayOrder = 0;
        }
    }
}