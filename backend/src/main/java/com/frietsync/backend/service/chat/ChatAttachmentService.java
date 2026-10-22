package com.frietsync.backend.service.chat;

import com.frietsync.backend.dto.chat.ChatAttachmentResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ChatAttachmentService {

    ChatAttachmentResponse upload(UUID projectId, UUID userId, MultipartFile file);

    List<ChatAttachmentResponse> bindToMessage(UUID projectId, UUID userId, UUID messageId, List<UUID> ids);

    AttachmentFile download(UUID projectId, UUID attachmentId, UUID userId);

    record AttachmentFile(String fileName, String contentType, Resource resource) { }
}
