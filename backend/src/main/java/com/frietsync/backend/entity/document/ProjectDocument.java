package com.frietsync.backend.entity.document;

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
@Table(name = "project_documents",
        indexes = @Index(name = "idx_project_documents_project", columnList = "project_id"))
public class ProjectDocument extends BaseEntity {

    @Column(name = "project_id", nullable = false, updatable = false)
    private UUID projectId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 50000)
    private String content;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "last_edited_by", nullable = false)
    private UUID lastEditedBy;
}