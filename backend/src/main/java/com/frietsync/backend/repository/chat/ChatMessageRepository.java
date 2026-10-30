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

    @Modifying
    @Query("update ChatMessage m set m.replyTo = null where m.replyTo.id = :messageId")
    void clearRepliesToMessage(@Param("messageId") UUID messageId);

    @Modifying
    @Query("delete from ChatMessage m where m.projectId = :projectId")
    void deleteByProjectId(@Param("projectId") UUID projectId);
}