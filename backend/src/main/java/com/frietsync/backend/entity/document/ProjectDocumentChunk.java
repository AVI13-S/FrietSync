package com.frietsync.backend.entity.document;

import com.frietsync.backend.entity.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "project_document_chunks",
        uniqueConstraints = @UniqueConstraint(columnNames = {"document_id", "chunk_index"}))
public class ProjectDocumentChunk extends BaseEntity {

    @Column(name = "document_id", nullable = false, updatable = false)
    private UUID documentId;

    @Column(name = "project_id", nullable = false, updatable = false)
    private UUID projectId;

    @Column(name = "chunk_index", nullable = false, updatable = false)
    private int chunkIndex;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "embedding", nullable = false, columnDefinition = "vector(1536)")
    private String embedding;
}
