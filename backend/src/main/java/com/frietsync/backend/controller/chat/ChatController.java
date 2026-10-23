package com.frietsync.backend.controller.chat;

import com.frietsync.backend.dto.chat.ChatMessageResponse;
import com.frietsync.backend.dto.chat.ChatAttachmentResponse;
import com.frietsync.backend.dto.chat.ChatAttachmentPageResponse;
import com.frietsync.backend.dto.chat.EditChatMessageRequest;
import com.frietsync.backend.exception.BadRequestException;
import com.frietsync.backend.service.chat.ChatAttachmentService.AttachmentFile;
import com.frietsync.backend.service.chat.ChatAttachmentService;
import com.frietsync.backend.service.chat.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/messages")
    public List<ChatMessageResponse> recentMessages(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "50") int limit) {
        if (limit < 1 || limit > 100) {
            throw new BadRequestException("Message limit must be between 1 and 100");
        }
        return chatService.getRecentMessages(projectId, userId, limit);
    }

    @PatchMapping("/messages/{messageId}")
    public ChatMessageResponse editMessage(
            @PathVariable UUID projectId,
            @PathVariable UUID messageId,
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody EditChatMessageRequest request) {
        ChatMessageResponse message = chatService.editMessage(projectId, userId, messageId, request);
        messagingTemplate.convertAndSend("/topic/projects/" + projectId + "/chat", message);
        return message;
    }

    @DeleteMapping("/messages/{messageId}")
    public ChatMessageResponse deleteMessage(
            @PathVariable UUID projectId,
            @PathVariable UUID messageId,
            @AuthenticationPrincipal UUID userId) {
        ChatMessageResponse message = chatService.deleteMessage(projectId, userId, messageId);
        messagingTemplate.convertAndSend("/topic/projects/" + projectId + "/chat", message);
        return message;
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