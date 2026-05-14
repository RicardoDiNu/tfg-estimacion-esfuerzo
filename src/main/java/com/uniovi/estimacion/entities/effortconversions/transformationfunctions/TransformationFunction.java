package com.uniovi.estimacion.entities.effortconversions.transformationfunctions;

import com.uniovi.estimacion.entities.users.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "transformation_functions")
@Getter
@Setter
@NoArgsConstructor
public class TransformationFunction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, length = 50)
    private String sourceTechniqueCode;

    @Column(nullable = false, length = 30)
    private String sourceSizeUnitCode;

    @Column(nullable = false)
    private Double intercept;

    @Column(nullable = false)
    private Double slope;

    @Column(nullable = false)
    private Boolean predefined = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public boolean isApplicableTo(String techniqueCode, String sizeUnitCode) {
        return sourceTechniqueCode != null
                && sourceSizeUnitCode != null
                && sourceTechniqueCode.equals(techniqueCode)
                && sourceSizeUnitCode.equals(sizeUnitCode);
    }

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.predefined == null) {
            this.predefined = false;
        }

        if (this.active == null) {
            this.active = true;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}