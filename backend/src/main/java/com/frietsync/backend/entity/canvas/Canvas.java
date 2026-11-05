package com.frietsync.backend.entity.canvas;

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
@Table(name = "canvases",
        indexes = @Index(name = "idx_canvases_project", columnList = "project_id"))
public class Canvas extends BaseEntity {

    @Column(name = "project_id", nullable = false, updatable = false)
    private UUID projectId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;
}