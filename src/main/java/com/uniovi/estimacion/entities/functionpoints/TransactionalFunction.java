package com.uniovi.estimacion.entities.functionpoints;

import com.uniovi.estimacion.entities.requirements.UserRequirement;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "transactional_functions")
@Getter
@Setter
@NoArgsConstructor
public class TransactionalFunction {

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
    private TransactionalFunctionType type;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FunctionPointComplexity complexity;

    @Column(nullable = false)
    private Integer weight;
}