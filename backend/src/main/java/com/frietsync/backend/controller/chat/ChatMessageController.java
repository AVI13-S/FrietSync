package com.frietsync.backend.controller.chat;

import com.frietsync.backend.dto.chat.ChatMessageRequest;
import com.frietsync.backend.dto.chat.ChatMessageResponse;
import com.frietsync.backend.service.chat.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@Validated
@RestController
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/projects/{projectId}/chat")
    public void sendMessage(@DestinationVariable UUID projectId,
                            @Valid ChatMessageRequest request,
                            Authentication authentication) {
        UUID senderId = extractUserId(authentication);
        ChatMessageResponse message = chatService.sendMessage(projectId, senderId, request);
        messagingTemplate.convertAndSend("/topic/projects/" + projectId + "/chat", message);
    }

    @MessageExceptionHandler
    public void handleException(Exception exception, SimpMessageHeaderAccessor accessor, Principal principal) {
        String errorPayload = "{\"error\": \"" + (exception.getMessage() != null ? exception.getMessage() : "Unknown error") + "\"}";
        if (principal != null && principal.getName() != null) {
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors", errorPayload);
        }
    }

    private UUID extractUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UUID id) {
            return id;
        }
        if (authentication != null && authentication.getPrincipal() instanceof String strId) {
            try {
                return UUID.fromString(strId);
            } catch (Exception ignored) {}
        }
        throw new IllegalArgumentException("User not authenticated");
    }
}
