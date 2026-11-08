package com.frietsync.backend.repository.document;

import com.frietsync.backend.entity.document.ProjectDocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProjectDocumentChunkRepository extends JpaRepository<ProjectDocumentChunk, UUID> {

    void deleteByDocumentId(UUID documentId);

    @Query(value = "SELECT d.id AS \"documentId\", d.title AS title, "
            + "LEFT(COALESCE(c.content, d.content), 1500) AS content "
            + "FROM project_documents d "
            + "LEFT JOIN project_document_chunks c ON c.document_id = d.id "
            + "WHERE d.project_id = :projectId AND (c.id IS NOT NULL OR d.embedding IS NOT NULL) "
            + "ORDER BY COALESCE(c.embedding, d.embedding) <=> CAST(:queryEmbedding AS vector) "
            + "LIMIT :limit",
            nativeQuery = true)
    List<DocumentChunkSearchResult> searchSimilarChunks(
            @Param("projectId") UUID projectId,
            @Param("queryEmbedding") String queryEmbedding,
            @Param("limit") int limit);
}
