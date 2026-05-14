package com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.actors;

import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.UseCasePointAnalysis;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "use_case_actors")
@Getter
@Setter
@NoArgsConstructor
public class UseCaseActor {

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UseCaseActorComplexity complexity;

    @Column(nullable = false)
    private Integer weight;
}