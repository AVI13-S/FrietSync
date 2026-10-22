package com.frietsync.backend.dto.chat;

import com.frietsync.backend.entity.chat.ChatAttachment;

import java.time.Instant;
import java.util.UUID;

public record ChatAttachmentResponse(
        UUID id,
        String fileName,
        String contentType,
        long sizeBytes,
        UUID uploadedBy,
        Instant createdAt,
        String downloadUrl
) {

    public static ChatAttachmentResponse fromEntity(ChatAttachment attachment) {
        return new ChatAttachmentResponse(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                attachment.getUploadedBy(),
                attachment.getCreatedAt(),
                "/api/v1/projects/" + attachment.getProjectId() + "/chat/attachments/"
                        + attachment.getId() + "/download"
        );
    }
}
