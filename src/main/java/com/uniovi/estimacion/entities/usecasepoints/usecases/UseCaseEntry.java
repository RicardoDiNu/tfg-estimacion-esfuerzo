package com.uniovi.estimacion.entities.usecasepoints.usecases;

import com.uniovi.estimacion.entities.usecasepoints.UseCasePointAnalysis;
import com.uniovi.estimacion.entities.usecasepoints.actors.UseCaseActor;
import com.uniovi.estimacion.entities.usecasepoints.modules.UseCasePointModule;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "use_case_entries")
@Getter
@Setter
@NoArgsConstructor
public class UseCaseEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "use_case_point_analysis_id", nullable = false)
    private UseCasePointAnalysis useCasePointAnalysis;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "use_case_point_module_id", nullable = false)
    private UseCasePointModule useCasePointModule;

    @ManyToMany
    @JoinTable(
            name = "use_case_entry_actors",
            joinColumns = @JoinColumn(name = "use_case_entry_id"),
            inverseJoinColumns = @JoinColumn(name = "actor_id")
    )
    private List<UseCaseActor> actors = new ArrayList<>();

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "trigger_condition", length = 1000)
    private String triggerCondition;

    @Column(length = 2000)
    private String preconditions;

    @Column(length = 2000)
    private String postconditions;

    @Column(name = "normal_flow", length = 4000)
    private String normalFlow;

    @Column(name = "alternative_flows", length = 4000)
    private String alternativeFlows;

    @Column(name = "exception_flows", length = 2000)
    private String exceptionFlows;

    @Column(name = "transaction_count")
    private Integer transactionCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UseCaseComplexity complexity;

    @Column(nullable = false)
    private Integer weight;

    public void addActor(UseCaseActor actor) {
        this.actors.add(actor);
    }

    public void removeActor(UseCaseActor actor) {
        this.actors.remove(actor);
    }
}