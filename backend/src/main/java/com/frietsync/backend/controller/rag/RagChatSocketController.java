package com.frietsync.backend.controller.rag;

import com.frietsync.backend.dto.rag.RagChatRequest;
import com.frietsync.backend.dto.rag.RagChatResponse;
import com.frietsync.backend.dto.rag.RagChatSocketEvent;
import com.frietsync.backend.exception.AiServiceException;
import com.frietsync.backend.exception.BadRequestException;
import com.frietsync.backend.exception.ForbiddenException;
import com.frietsync.backend.service.rag.RagChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequiredArgsConstructor
public class RagChatSocketController {

    private static final Logger LOG = LoggerFactory.getLogger(RagChatSocketController.class);
    private static final String USER_QUEUE = "/queue/ai-chat";

    private final RagChatService ragChatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/projects/{projectId}/rag-chat")
    public void ask(@DestinationVariable UUID projectId,
                    @Valid RagChatRequest request,
                    Authentication authentication) {
        UUID userId = currentUserId(authentication);
        UUID requestId = UUID.randomUUID();
        send(userId, new RagChatSocketEvent("started", requestId, null, null));

        try {
            RagChatResponse response = ragChatService.askRagChatbot(projectId, request.getQuestion(), userId);
            send(userId, new RagChatSocketEvent("completed", requestId, response, null));
        } catch (RuntimeException ex) {
            LOG.error("WebSocket AI chat failed for user {} in project {}", userId, projectId, ex);
            String message = ex instanceof BadRequestException
                    || ex instanceof AiServiceException
                    || ex instanceof ForbiddenException
                    ? ex.getMessage()
                    : "The AI chat request failed. Please try again.";
            send(userId, new RagChatSocketEvent("error", requestId, null, message));
        }
    }

    private UUID currentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UUID userId) {
            return userId;
        }
        throw new IllegalArgumentException("User not authenticated");
    }

    private void send(UUID userId, RagChatSocketEvent event) {
        messagingTemplate.convertAndSendToUser(userId.toString(), USER_QUEUE, event);
    }
}
