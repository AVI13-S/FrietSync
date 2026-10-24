package com.frietsync.backend.dto.chat;

import com.frietsync.backend.entity.chat.ChatMessage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id,
        UUID projectId,
        UUID senderId,
        String senderName,
        String content,
        List<ChatAttachmentResponse> attachments,
        Instant createdAt,
        UUID replyToId,
        String replyToSenderName,
        String replyToContent,
        List<UUID> mentionedUserIds,
        boolean deleted,
        Instant editedAt
) {

    public static ChatMessageResponse fromEntity(ChatMessage message) {
        return fromEntity(message, List.of());
    }

    public static ChatMessageResponse fromEntity(
            ChatMessage message, List<ChatAttachmentResponse> attachments) {
        boolean deleted = message.isDeleted();
        UUID replyToId = null;
        String replyToSenderName = null;
        String replyToContent = null;

        ChatMessage parent = message.getReplyTo();
        if (parent != null) {
            replyToId = parent.getId();
            replyToSenderName = parent.getSenderName();
            replyToContent = parent.isDeleted() ? null : parent.getContent();
            if (replyToContent != null && replyToContent.length() > 100) {
                replyToContent = replyToContent.substring(0, 100) + "...";
            }
        }

        return new ChatMessageResponse(
                message.getId(),
                message.getProjectId(),
                message.getSenderId(),
                message.getSenderName(),
                deleted ? "" : message.getContent(),
                deleted ? List.of() : attachments,
                message.getCreatedAt(),
                replyToId,
                replyToSenderName,
                replyToContent,
                deleted ? new ArrayList<>() : new ArrayList<>(message.getMentionedUserIds()),
                deleted,
                message.getEditedAt()
        );
    }
}