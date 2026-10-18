package com.frietsync.backend.repository.document;

import com.frietsync.backend.entity.document.ProjectDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectDocumentRepository extends JpaRepository<ProjectDocument, UUID> {

    List<ProjectDocument> findByProjectIdOrderByUpdatedAtDesc(UUID projectId);

    void deleteByProjectId(UUID projectId);
}