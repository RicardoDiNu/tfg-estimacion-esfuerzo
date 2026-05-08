package com.uniovi.estimacion.entities.usecasepoints.modules;

import com.uniovi.estimacion.entities.usecasepoints.UseCasePointAnalysis;
import com.uniovi.estimacion.entities.usecasepoints.usecases.UseCaseEntry;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "use_case_point_modules")
@Getter
@Setter
@NoArgsConstructor
public class UseCasePointModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "use_case_point_analysis_id", nullable = false)
    private UseCasePointAnalysis useCasePointAnalysis;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    @OneToMany(mappedBy = "useCasePointModule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UseCaseEntry> useCases = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public void addUseCase(UseCaseEntry useCase) {
        useCase.setUseCasePointModule(this);
        this.useCases.add(useCase);
    }

    public void removeUseCase(UseCaseEntry useCase) {
        this.useCases.remove(useCase);
        useCase.setUseCasePointModule(null);
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