package com.uniovi.estimacion.entities.effortconversions.delphi;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "delphi_iterations")
@Getter
@Setter
@NoArgsConstructor
public class DelphiIteration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "delphi_estimation_id", nullable = false)
    private DelphiEstimation delphiEstimation;

    @Column(nullable = false)
    private Integer iterationNumber;

    @Column
    private Double minimumModuleDeviationPercentage;

    @Column
    private Double maximumModuleDeviationPercentage;

    @Column(nullable = false)
    private Boolean acceptedByDeviation = false;

    @Column(nullable = false)
    private Boolean acceptedAsLastIteration = false;

    @Column(nullable = false)
    private Boolean finalIteration = false;

    @OneToMany(mappedBy = "delphiIteration", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DelphiExpertEstimate> expertEstimates = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void addExpertEstimate(DelphiExpertEstimate expertEstimate) {
        expertEstimate.setDelphiIteration(this);
        this.expertEstimates.add(expertEstimate);
    }

    public void removeExpertEstimate(DelphiExpertEstimate expertEstimate) {
        this.expertEstimates.remove(expertEstimate);
        expertEstimate.setDelphiIteration(null);
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}