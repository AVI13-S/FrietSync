package com.frietsync.backend.controller.rag;

import com.frietsync.backend.dto.rag.RagChatRequest;
import com.frietsync.backend.dto.rag.RagChatResponse;
import com.frietsync.backend.dto.rag.RagChatSocketEvent;
import com.frietsync.backend.exception.AiServiceException;
import com.frietsync.backend.exception.BadRequestException;
import com.frietsync.backend.exception.ForbiddenException;
import com.frietsync.backend.service.rag.RagChatService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;

@Validated
@RestController
public class RagChatSocketController {

    private static final Logger LOG = LoggerFactory.getLogger(RagChatSocketController.class);
    private static final String USER_QUEUE = "/queue/ai-chat";

    private final RagChatService ragChatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final Executor ragChatExecutor;

    public RagChatSocketController(RagChatService ragChatService,
                                   SimpMessagingTemplate messagingTemplate,
                                   @Qualifier("ragChatExecutor") Executor ragChatExecutor) {
        this.ragChatService = ragChatService;
        this.messagingTemplate = messagingTemplate;
        this.ragChatExecutor = ragChatExecutor;
    }

    @MessageMapping("/ai-chat")
    public void ask(@Valid RagChatRequest request,
                    Authentication authentication) {
        UUID userId = currentUserId(authentication);
        UUID requestId = request.getRequestId() != null ? request.getRequestId() : UUID.randomUUID();
        send(userId, new RagChatSocketEvent("started", requestId, null, null, null));

        try {
            ragChatExecutor.execute(() -> processQuestion(request, userId, requestId));
        } catch (RejectedExecutionException ex) {
            LOG.warn("RAG chat executor is saturated for user {}", userId);
            send(userId, new RagChatSocketEvent(
                    "error", requestId, null, "AI chat is busy. Please try again shortly.", null));
        }
    }

    @MessageExceptionHandler
    public void handleMessageException(Exception exception,
                                       Authentication authentication,
                                       Message<?> message) {
        UUID userId = currentUserId(authentication);
        UUID requestId = requestIdFrom(message);
        String error = exception instanceof MethodArgumentNotValidException
                ? "Question is required and must be at most 1000 characters."
                : "The AI chat request could not be processed. Please try again.";
        if (!(exception instanceof MethodArgumentNotValidException)
                && !(exception instanceof MessagingException)) {
            LOG.warn("RAG WebSocket request failed for user {}", userId, exception);
        }
        send(userId, new RagChatSocketEvent("error", requestId, null, error, null));
    }

    private void processQuestion(RagChatRequest request, UUID userId, UUID requestId) {
        try {
            Consumer<String> chunkHandler = piece ->
                    send(userId, new RagChatSocketEvent("chunk", requestId, null, null, piece));
            RagChatResponse response = ragChatService.askRagChatbotStreaming(
                    request.getProjectId(), request.getQuestion(), userId, chunkHandler);
            send(userId, new RagChatSocketEvent("completed", requestId, response, null, null));
        } catch (RuntimeException ex) {
            LOG.error("WebSocket AI chat failed for user {}", userId, ex);
            String message = ex instanceof BadRequestException
                    || ex instanceof AiServiceException
                    || ex instanceof ForbiddenException
                    ? ex.getMessage()
                    : "The AI chat request failed. Please try again.";
            send(userId, new RagChatSocketEvent("error", requestId, null, message, null));
        }
    }

    private UUID requestIdFrom(Message<?> message) {
        if (message != null && message.getPayload() instanceof RagChatRequest request) {
            return request.getRequestId();
        }
        return null;
    }

    private UUID currentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UUID userId) {
            return userId;
        }
        if (authentication != null && authentication.getPrincipal() instanceof String strId) {
            try {
                return UUID.fromString(strId);
            } catch (Exception ignored) {}
        }
        throw new IllegalArgumentException("User not authenticated");
    }

    private void send(UUID userId, RagChatSocketEvent event) {
        messagingTemplate.convertAndSendToUser(userId.toString(), USER_QUEUE, event);
    }
}
