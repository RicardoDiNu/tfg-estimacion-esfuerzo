package com.uniovi.estimacion.entities.projects;

import com.uniovi.estimacion.entities.users.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "project_memberships",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"project_id", "worker_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ProjectMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private EstimationProject project;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private User worker;

    @Column(nullable = false)
    private Boolean canEditEstimationData = false;

    @Column(nullable = false)
    private Boolean canManageEffortConversions = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public ProjectMembership(EstimationProject project,
                             User worker,
                             Boolean canEditEstimationData,
                             Boolean canManageEffortConversions) {
        this.project = project;
        this.worker = worker;
        this.canEditEstimationData = Boolean.TRUE.equals(canEditEstimationData);
        this.canManageEffortConversions = Boolean.TRUE.equals(canManageEffortConversions);
    }

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        normalizePermissions();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();

        normalizePermissions();
    }

    private void normalizePermissions() {
        this.canEditEstimationData = Boolean.TRUE.equals(this.canEditEstimationData);
        this.canManageEffortConversions = Boolean.TRUE.equals(this.canManageEffortConversions);
    }
}