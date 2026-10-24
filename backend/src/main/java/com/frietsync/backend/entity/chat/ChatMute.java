package com.frietsync.backend.entity.chat;

import com.frietsync.backend.entity.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "chat_mutes",
        uniqueConstraints = @UniqueConstraint(name = "uk_chat_mutes_project_user",
                columnNames = {"project_id", "user_id"}),
        indexes = @Index(name = "idx_chat_mutes_project", columnList = "project_id"))
public class ChatMute extends BaseEntity {

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "muted_until")
    private Instant mutedUntil;
}