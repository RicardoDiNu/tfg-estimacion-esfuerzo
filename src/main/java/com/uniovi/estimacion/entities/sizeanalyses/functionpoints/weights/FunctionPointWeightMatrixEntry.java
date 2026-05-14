package com.uniovi.estimacion.entities.sizeanalyses.functionpoints.weights;

import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.FunctionPointAnalysis;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.FunctionPointComplexity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "function_point_weight_matrix_entries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_fp_weight_matrix_entry",
                columnNames = {
                        "function_point_analysis_id",
                        "function_type",
                        "complexity"
                }
        )
)
@Getter
@Setter
@NoArgsConstructor
public class FunctionPointWeightMatrixEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "function_point_analysis_id", nullable = false)
    private FunctionPointAnalysis functionPointAnalysis;

    @Enumerated(EnumType.STRING)
    @Column(name = "function_type", nullable = false, length = 10)
    private FunctionPointFunctionType functionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FunctionPointComplexity complexity;

    @Column(nullable = false)
    private Integer weight = 0;

    @Column(nullable = false)
    private Integer displayOrder = 0;
}