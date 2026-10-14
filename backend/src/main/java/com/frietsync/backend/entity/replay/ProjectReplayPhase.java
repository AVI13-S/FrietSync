package com.frietsync.backend.entity.replay;

import com.frietsync.backend.entity.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "project_replay_phases",
        uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "phase_type"}))
public class ProjectReplayPhase extends BaseEntity {

    @Column(name = "project_id", nullable = false, updatable = false)
    private UUID projectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "phase_type", nullable = false, updatable = false, length = 20)
    private ReplayPhaseType type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 500)
    private String description;
}