package com.frietsync.backend.entity.notification;

import com.frietsync.backend.entity.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "notifications",
        indexes = @Index(name = "idx_notifications_user", columnList = "user_id, created_at"))
public class Notification extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String message;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "issue_id")
    private UUID issueId;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "is_read", nullable = false)
    private boolean seen = false;

    @Column(name = "read_at")
    private Instant readAt;
}