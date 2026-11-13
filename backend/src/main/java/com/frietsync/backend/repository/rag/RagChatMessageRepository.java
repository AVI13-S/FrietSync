package com.frietsync.backend.repository.rag;

import com.frietsync.backend.entity.rag.RagChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface RagChatMessageRepository extends JpaRepository<RagChatMessage, UUID> {

    List<RagChatMessage> findTop5ByProjectIdAndUserIdAndCreatedAtAfterOrderByCreatedAtDesc(
            UUID projectId, UUID userId, Instant since);

    List<RagChatMessage> findTop5ByUserIdAndWorkspaceIdAndCreatedAtAfterOrderByCreatedAtDesc(
            UUID userId, UUID workspaceId, Instant since);

    @Modifying
    @Query("delete from RagChatMessage m where m.projectId = :projectId")
    void deleteByProjectId(@Param("projectId") UUID projectId);
}
