package com.frietsync.backend.entity.activity;

import com.frietsync.backend.entity.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "project_activity", indexes = {
        @Index(name = "idx_project_activity_project_created", columnList = "project_id, created_at")
})
public class ProjectActivity extends BaseEntity {

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Column(name = "resource_type", nullable = false, length = 30)
    private String resourceType;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "resource_name", nullable = false, length = 300)
    private String resourceName;

    @Column(nullable = false, length = 20)
    private String action;
}
