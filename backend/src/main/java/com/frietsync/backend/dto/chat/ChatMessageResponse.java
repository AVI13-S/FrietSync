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
        Instant createdAt,
        UUID replyToId,
        String replyToSenderName,
        String replyToContent,
        List<UUID> mentionedUserIds
) {

    public static ChatMessageResponse fromEntity(ChatMessage message) {
        UUID replyToId = null;
        String replyToSenderName = null;
        String replyToContent = null;

        ChatMessage parent = message.getReplyTo();
        if (parent != null) {
            replyToId = parent.getId();
            replyToSenderName = parent.getSenderName();
            replyToContent = parent.getContent();
            if (replyToContent.length() > 100) {
                replyToContent = replyToContent.substring(0, 100) + "...";
            }
        }

        return new ChatMessageResponse(
                message.getId(),
                message.getProjectId(),
                message.getSenderId(),
                message.getSenderName(),
                message.getContent(),
                attachments,
                message.getCreatedAt()
                replyToId,
                replyToSenderName,
                replyToContent,
                new ArrayList<>(message.getMentionedUserIds())
        );
    }
}