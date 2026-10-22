package com.frietsync.backend.dto.chat;

import com.frietsync.backend.entity.chat.ChatMessage;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id,
        UUID projectId,
        UUID senderId,
        String senderName,
        String content,
        List<ChatAttachmentResponse> attachments,
        Instant createdAt
) {

    public static ChatMessageResponse fromEntity(ChatMessage message) {
        return fromEntity(message, List.of());
    }

    public static ChatMessageResponse fromEntity(
            ChatMessage message, List<ChatAttachmentResponse> attachments) {
        return new ChatMessageResponse(
                message.getId(),
                message.getProjectId(),
                message.getSenderId(),
                message.getSenderName(),
                message.getContent(),
                attachments,
                message.getCreatedAt()
        );
    }
}
