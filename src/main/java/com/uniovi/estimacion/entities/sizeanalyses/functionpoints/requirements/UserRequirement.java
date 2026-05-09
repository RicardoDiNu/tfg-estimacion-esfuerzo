package com.uniovi.estimacion.entities.sizeanalyses.functionpoints.requirements;

import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.FunctionPointAnalysis;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.modules.FunctionPointModule;
import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.DataFunction;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.TransactionalFunction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_requirements")
@Getter
@Setter
@NoArgsConstructor
public class UserRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "function_point_module_id", nullable = false)
    private FunctionPointModule functionPointModule;

    @Column(length = 50, nullable = false)
    private String identifier;

    @Column(name = "statement", length = 2000, nullable = false)
    private String statement;

    @OneToMany(mappedBy = "userRequirement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DataFunction> dataFunctions = new ArrayList<>();

    @OneToMany(mappedBy = "userRequirement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransactionalFunction> transactionalFunctions = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

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

    @Transient
    public FunctionPointAnalysis getFunctionPointAnalysis() {
        return functionPointModule != null
                ? functionPointModule.getFunctionPointAnalysis()
                : null;
    }

    @Transient
    public EstimationProject getEstimationProject() {
        FunctionPointAnalysis analysis = getFunctionPointAnalysis();

        return analysis != null
                ? analysis.getEstimationProject()
                : null;
    }
}