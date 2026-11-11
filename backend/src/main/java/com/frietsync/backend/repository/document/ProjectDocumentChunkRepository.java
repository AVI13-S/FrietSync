package com.frietsync.backend.repository.document;

import com.frietsync.backend.entity.document.ProjectDocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProjectDocumentChunkRepository extends JpaRepository<ProjectDocumentChunk, UUID> {

    void deleteByDocumentId(UUID documentId);

    @Query(value = "SELECT * FROM ("
            + "SELECT d.id AS \"documentId\", d.title AS title, "
            + "LEFT(COALESCE(c.content, d.content), 1500) AS content, "
            + "1 - (COALESCE(c.embedding, d.embedding) <=> CAST(:queryEmbedding AS vector)) AS similarity "
            + "FROM project_documents d "
            + "LEFT JOIN project_document_chunks c ON c.document_id = d.id "
            + "WHERE d.project_id = :projectId AND (c.id IS NOT NULL OR d.embedding IS NOT NULL)"
            + ") AS matches "
            + "WHERE similarity >= :minSimilarity "
            + "ORDER BY similarity DESC "
            + "LIMIT :limit",
            nativeQuery = true)
    List<DocumentChunkSearchResult> searchSimilarChunks(
            @Param("projectId") UUID projectId,
            @Param("queryEmbedding") String queryEmbedding,
            @Param("limit") int limit,
            @Param("minSimilarity") double minSimilarity);

    @Query(value = "SELECT * FROM ("
            + "SELECT DISTINCT ON (d.id) d.id AS \"documentId\", d.project_id AS \"projectId\", "
            + "p.name AS \"projectName\", d.title AS title, LEFT(c.content, 300) AS description, "
            + "1 - (c.embedding <=> CAST(:queryEmbedding AS vector)) AS similarity "
            + "FROM project_document_chunks c "
            + "JOIN project_documents d ON d.id = c.document_id "
            + "JOIN projects p ON p.id = d.project_id "
            + "JOIN users u ON u.id = :userId AND u.workspace_id = p.workspace_id "
            + "JOIN workspaces w ON w.id = p.workspace_id AND w.deletion_requested_at IS NULL "
            + "WHERE p.deleted_at IS NULL "
            + "AND (CAST(:projectId AS varchar) IS NULL "
            + "OR p.id = CAST(CAST(:projectId AS varchar) AS uuid)) "
            + "AND (p.admin_id = :userId OR p.project_manager_id = :userId OR p.team_lead_id = :userId "
            + "OR EXISTS (SELECT 1 FROM project_members pm "
            + "WHERE pm.project_id = p.id AND pm.user_id = :userId)) "
            + "ORDER BY d.id, c.embedding <=> CAST(:queryEmbedding AS vector)"
            + ") AS best ORDER BY similarity DESC LIMIT :limit",
            nativeQuery = true)
    List<DocumentSemanticSearchResult> searchDocumentsForUser(
            @Param("userId") UUID userId,
            @Param("queryEmbedding") String queryEmbedding,
            @Param("projectId") String projectId,
            @Param("limit") int limit);
}