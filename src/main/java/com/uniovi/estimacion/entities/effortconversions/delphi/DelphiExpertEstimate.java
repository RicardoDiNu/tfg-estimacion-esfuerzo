package com.uniovi.estimacion.entities.effortconversions.delphi;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "delphi_expert_estimates")
@Getter
@Setter
@NoArgsConstructor
public class DelphiExpertEstimate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "delphi_iteration_id", nullable = false)
    private DelphiIteration delphiIteration;

    @Column(nullable = false, length = 150)
    private String evaluatorAlias;

    @Column(nullable = false)
    private Double minimumModuleEstimatedEffortHours;

    @Column(nullable = false)
    private Double maximumModuleEstimatedEffortHours;

    @Column(length = 2000)
    private String comments;
}