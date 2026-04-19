package com.uniovi.estimacion.entities.requirements;

import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.functionpoints.DataFunction;
import com.uniovi.estimacion.entities.functionpoints.TransactionalFunction;
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
    @JoinColumn(name = "estimation_project_id", nullable = false)
    private EstimationProject estimationProject;

    @Column(length = 50)
    private String identifier;

    @Column(length = 200)
    private String name;

    @Column(length = 2000)
    private String description;

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
}