package com.frietsync.backend.controller.chat;

import com.frietsync.backend.dto.chat.ChatMessageResponse;
import com.frietsync.backend.service.chat.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/chat")
@RequiredArgsConstructor
public class ChatPinController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/pinned-messages")
    public List<ChatMessageResponse> pinnedMessages(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UUID userId) {
        return chatService.getPinnedMessages(projectId, userId);
    }

    @PutMapping("/messages/{messageId}/pin")
    public ChatMessageResponse pin(
            @PathVariable UUID projectId,
            @PathVariable UUID messageId,
            @AuthenticationPrincipal UUID userId) {
        ChatMessageResponse message = chatService.pinMessage(projectId, userId, messageId);
        messagingTemplate.convertAndSend("/topic/projects/" + projectId + "/chat", message);
        return message;
    }

    @DeleteMapping("/messages/{messageId}/pin")
    public ChatMessageResponse unpin(
            @PathVariable UUID projectId,
            @PathVariable UUID messageId,
            @AuthenticationPrincipal UUID userId) {
        ChatMessageResponse message = chatService.unpinMessage(projectId, userId, messageId);
        messagingTemplate.convertAndSend("/topic/projects/" + projectId + "/chat", message);
        return message;
    }
}