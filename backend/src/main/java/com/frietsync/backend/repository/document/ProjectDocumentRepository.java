package com.frietsync.backend.repository.document;

import com.frietsync.backend.entity.document.ProjectDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProjectDocumentRepository extends JpaRepository<ProjectDocument, UUID> {

    List<ProjectDocument> findByProjectIdOrderByUpdatedAtDesc(UUID projectId);

    void deleteByProjectId(UUID projectId);

    @Query(value = "SELECT * FROM project_documents WHERE project_id = :projectId " +
                   "ORDER BY embedding <=> CAST(:queryEmbedding AS vector) LIMIT :limit",
           nativeQuery = true)
    List<ProjectDocument> searchSimilarDocuments(
            @Param("projectId") UUID projectId,
            @Param("queryEmbedding") String queryEmbedding,
            @Param("limit") int limit);
}