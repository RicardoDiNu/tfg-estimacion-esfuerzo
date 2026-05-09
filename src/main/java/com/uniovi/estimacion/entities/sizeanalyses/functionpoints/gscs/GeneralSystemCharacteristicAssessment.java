package com.uniovi.estimacion.entities.sizeanalyses.functionpoints.gscs;

import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.FunctionPointAnalysis;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "general_system_characteristic_assessments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"function_point_analysis_id", "characteristic_type"})
)
@Getter
@Setter
@NoArgsConstructor
public class GeneralSystemCharacteristicAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "function_point_analysis_id", nullable = false)
    private FunctionPointAnalysis functionPointAnalysis;

    @Enumerated(EnumType.STRING)
    @Column(name = "characteristic_type", nullable = false, length = 50)
    private GeneralSystemCharacteristicType characteristicType;

    @Column(nullable = false)
    private Integer degreeOfInfluence;
}