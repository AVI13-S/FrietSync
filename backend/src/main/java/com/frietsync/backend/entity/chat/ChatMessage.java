package com.frietsync.backend.entity.chat;

import com.frietsync.backend.entity.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.CollectionTable;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "chat_messages",
        indexes = {
                @Index(name = "idx_chat_messages_project_user", columnList = "project_id, user_id"),
                @Index(name = "idx_chat_messages_user_created", columnList = "user_id, created_at DESC")
        })
public class ChatMessage extends BaseEntity {

    @Column(name = "project_id", nullable = false, length = 2000)
    private UUID projectId;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(name = "sender_name", nullable = false)
    private String senderName;

    @Column(nullable = false, length = 2000)
    private String content;

    private boolean deleted;

    @Column(name = "forwarded", nullable = false)
    private boolean forwarded = false;

    @Column(name = "forwarded_from_sender_name")
    private String forwardedFromSenderName;

    @Column(name = "edited_at")
    private Instant editedAt;

    @Column(name = "deleted_at", nullable = true)
    private Instant deletedAt;

    @Column(name = "deleted_by", nullable = true)
    private UUID deletedBy;

    @Column(name = "pinned_at", nullable = true)
    private Instant pinnedAt;

    @Column(name = "pinned_by", nullable = true)
    private UUID pinnedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_id", nullable = true)
    private ChatMessage replyTo;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "chat_message_mentions", joinColumns = @JoinColumn(name = "message_id"))
    @Column(name = "user_id", nullable = false)
    private Set<UUID> mentionedUserIds = new HashSet<>();
}