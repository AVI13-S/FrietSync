package com.frietsync.backend.repository.chat;

import com.frietsync.backend.entity.chat.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findByProjectId(
            UUID projectId,
            Pageable pageable
    );

    @Query("select m from ChatMessage m where m.projectId = :projectId and m.deleted = false "
            + "and lower(m.content) like lower(concat('%', :query, '%'))")
    Page<ChatMessage> searchMessages(
            @Param("projectId") UUID projectId,
            @Param("query") String query,
            Pageable pageable);

    @Query("select m from ChatMessage m where m.projectId = :projectId and m.createdAt < :createdAt "
            + "or (m.projectId = :projectId and m.createdAt = :createdAt and m.id < :messageId)")
    List<ChatMessage> findOlderMessages(
            @Param("projectId") UUID projectId,
            @Param("createdAt") java.time.Instant createdAt,
            @Param("messageId") UUID messageId,
            Pageable pageable);

    @Query("select m from ChatMessage m where m.projectId = :projectId and m.createdAt > :createdAt "
            + "or (m.projectId = :projectId and m.createdAt = :createdAt and m.id > :messageId)")
    List<ChatMessage> findNewerMessages(
            @Param("projectId") UUID projectId,
            @Param("createdAt") java.time.Instant createdAt,
            @Param("messageId") UUID messageId,
            Pageable pageable);


    List<ChatMessage> findByProjectIdAndPinnedAtIsNotNullAndDeletedFalseOrderByPinnedAtDesc(UUID projectId);

    long countByProjectIdAndPinnedAtIsNotNullAndDeletedFalse(UUID projectId);

    @Query(value = "SELECT * FROM chat_messages WHERE embedding IS NULL AND deleted = false "
            + "AND LENGTH(content) >= 15 ORDER BY created_at DESC LIMIT :limit",
            nativeQuery = true)
    List<ChatMessage> findMessagesWithoutEmbedding(@Param("limit") int limit);

    @Modifying
    @Query(value = "UPDATE chat_messages SET embedding = CAST(:embedding AS vector) "
            + "WHERE id = :messageId AND embedding IS NULL",
            nativeQuery = true)
    int updateEmbeddingIfMissing(@Param("messageId") UUID messageId, @Param("embedding") String embedding);

    @Modifying
    @Query(value = "UPDATE chat_messages SET embedding = NULL WHERE id = :messageId",
            nativeQuery = true)
    int clearEmbedding(@Param("messageId") UUID messageId);

    @Query(value = "SELECT m.id AS \"messageId\", m.project_id AS \"projectId\", p.name AS \"projectName\", "
            + "m.sender_name AS title, LEFT(m.content, 300) AS description, "
            + "1 - (m.embedding <=> CAST(:queryEmbedding AS vector)) AS similarity "
            + "FROM chat_messages m JOIN projects p ON p.id = m.project_id "
            + "JOIN users u ON u.id = :userId AND u.workspace_id = p.workspace_id "
            + "JOIN workspaces w ON w.id = p.workspace_id AND w.deletion_requested_at IS NULL "
            + "WHERE m.embedding IS NOT NULL AND m.deleted = false AND p.deleted_at IS NULL "
            + "AND (CAST(:projectId AS varchar) IS NULL "
            + "OR p.id = CAST(CAST(:projectId AS varchar) AS uuid)) "
            + "AND (p.admin_id = :userId OR p.project_manager_id = :userId OR p.team_lead_id = :userId "
            + "OR EXISTS (SELECT 1 FROM project_members pm "
            + "WHERE pm.project_id = p.id AND pm.user_id = :userId)) "
            + "ORDER BY m.embedding <=> CAST(:queryEmbedding AS vector) LIMIT :limit",
            nativeQuery = true)
    List<ChatMessageSemanticSearchResult> searchSemanticallyForUser(
            @Param("userId") UUID userId,
            @Param("queryEmbedding") String queryEmbedding,
            @Param("projectId") String projectId,
            @Param("limit") int limit);

    @Modifying
    @Query("update ChatMessage m set m.replyTo = null where m.replyTo.id = :messageId")
    void clearRepliesToMessage(@Param("messageId") UUID messageId);

    @Modifying
    @Query("delete from ChatMessage m where m.projectId = :projectId")
    void deleteByProjectId(@Param("projectId") UUID projectId);
}