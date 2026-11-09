package com.frietsync.backend.entity.rag;

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
@Table(name = "rag_chat_messages",
        indexes = @Index(name = "idx_rag_chat_messages_project_user", columnList = "project_id, user_id"))
public class RagChatMessage extends BaseEntity {

    @Column(name = "project_id", nullable = false, updatable = false)
    private UUID projectId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false, length = 1000, updatable = false)
    private String question;

    @Column(nullable = false, columnDefinition = "text", updatable = false)
    private String answer;

    @Column(length = 10)
    private String feedback;
}