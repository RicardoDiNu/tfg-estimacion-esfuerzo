package com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.factors;

import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.UseCasePointAnalysis;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "technical_factor_assessments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"use_case_point_analysis_id", "factor_type"})
)
@Getter
@Setter
@NoArgsConstructor
public class TechnicalFactorAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "use_case_point_analysis_id", nullable = false)
    private UseCasePointAnalysis useCasePointAnalysis;

    @Enumerated(EnumType.STRING)
    @Column(name = "factor_type", nullable = false, length = 50)
    private TechnicalFactorType factorType;

    @Column(nullable = false)
    private Integer degreeOfInfluence;
}