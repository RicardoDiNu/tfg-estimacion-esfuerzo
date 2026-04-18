package com.uniovi.estimacion.entities.functionpoints;

import com.uniovi.estimacion.entities.requirements.UserRequirement;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "data_functions")
@Getter
@Setter
@NoArgsConstructor
public class DataFunction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "function_point_analysis_id", nullable = false)
    private FunctionPointAnalysis functionPointAnalysis;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_requirement_id")
    private UserRequirement userRequirement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DataFunctionType type;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Integer detCount;

    @Column(nullable = false)
    private Integer retCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FunctionPointComplexity complexity;

    @Column(nullable = false)
    private Integer weight;
}