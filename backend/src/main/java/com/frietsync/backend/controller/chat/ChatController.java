package com.frietsync.backend.controller.chat;

import com.frietsync.backend.dto.chat.ChatMessageResponse;
import com.frietsync.backend.dto.chat.ChatMessagePageResponse;
import com.frietsync.backend.dto.chat.ChatMessageWindowResponse;
import com.frietsync.backend.dto.chat.ChatAttachmentResponse;
import com.frietsync.backend.dto.chat.ChatAttachmentPageResponse;
import com.frietsync.backend.exception.BadRequestException;
import com.frietsync.backend.service.chat.ChatAttachmentService.AttachmentFile;
import com.frietsync.backend.service.chat.ChatAttachmentService;
import com.frietsync.backend.service.chat.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatAttachmentService attachmentService;

    @GetMapping("/messages")
    public List<ChatMessageResponse> recentMessages(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        if (page < 0) {
            throw new BadRequestException("Message page cannot be negative");
        }
        if (size < 1 || size > 100) {
            throw new BadRequestException("Message page size must be between 1 and 100");
        }
        return chatService.getRecentMessages(projectId, userId, page, size);
    }

    @GetMapping("/messages/search")
    public ResponseEntity<ChatMessagePageResponse> searchMessages(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UUID userId,
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (q.isBlank() || q.length() > 200) {
            throw new BadRequestException("Search query must contain 1 to 200 characters");
        }
        if (page < 0) {
            throw new BadRequestException("Message page cannot be negative");
        }
        if (size < 1 || size > 100) {
            throw new BadRequestException("Message page size must be between 1 and 100");
        }
        return ResponseEntity.ok(chatService.searchMessages(projectId, userId, q, page, size));
    }

    @GetMapping("/messages/around/{messageId}")
    public ResponseEntity<ChatMessageWindowResponse> messagesAround(
            @PathVariable UUID projectId,
            @PathVariable UUID messageId,
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "25") int before,
            @RequestParam(defaultValue = "25") int after) {
        if (before < 0 || before > 50 || after < 0 || after > 50) {
            throw new BadRequestException("Before and after must each be between 0 and 50");
        }
        return ResponseEntity.ok(chatService.getMessagesAround(projectId, userId, messageId, before, after));
    }

    @PostMapping(value = "/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ChatAttachmentResponse> uploadAttachment(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UUID userId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(201).body(attachmentService.upload(projectId, userId, file));
    }

    @GetMapping("/attachments")
    public ResponseEntity<ChatAttachmentPageResponse> projectAttachments(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(attachmentService.listProjectAttachments(projectId, userId, page));
    }

    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable UUID projectId,
            @PathVariable UUID attachmentId,
            @AuthenticationPrincipal UUID userId) {
        AttachmentFile file = attachmentService.download(projectId, attachmentId, userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.fileName(), java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(file.resource());
    }
}
