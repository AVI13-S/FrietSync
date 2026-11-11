package com.frietsync.backend.controller.rag;

import com.frietsync.backend.dto.rag.RagChatRequest;
import com.frietsync.backend.dto.rag.RagChatResponse;
import com.frietsync.backend.dto.rag.RagChatSocketEvent;
import com.frietsync.backend.service.rag.RagChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagChatSocketControllerTest {

    @Mock
    private RagChatService ragChatService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private final List<Runnable> queuedTasks = new ArrayList<>();
    private RagChatSocketController controller;

    @BeforeEach
    void setUp() {
        queuedTasks.clear();
        controller = new RagChatSocketController(ragChatService, messagingTemplate, queuedTasks::add);
    }

    @Test
    void runsQuestionOffInboundThreadAndUsesClientRequestIdForEvents() {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        RagChatRequest request = new RagChatRequest();
        request.setRequestId(requestId);
        request.setQuestion("How is this project deployed?");
        RagChatResponse response = new RagChatResponse();
        response.setAnswer("Use the deployment workflow.");
        when(ragChatService.askRagChatbotStreaming(
                eq(projectId), eq(request.getQuestion()), eq(userId), any()))
                .thenReturn(response);

        controller.ask(projectId, request, authentication(userId));

        assertThat(queuedTasks).hasSize(1);
        verifyNoInteractions(ragChatService);
        queuedTasks.remove(0).run();

        ArgumentCaptor<RagChatSocketEvent> eventCaptor = ArgumentCaptor.forClass(RagChatSocketEvent.class);
        verify(messagingTemplate, org.mockito.Mockito.times(2)).convertAndSendToUser(
                eq(userId.toString()), eq("/queue/ai-chat"), eventCaptor.capture());
        assertThat(eventCaptor.getAllValues())
                .extracting(RagChatSocketEvent::type)
                .containsExactly("started", "completed");
        assertThat(eventCaptor.getAllValues())
                .extracting(RagChatSocketEvent::requestId)
                .containsOnly(requestId);
    }

    @Test
    void sendsValidationFailureToUserQueue() {
        UUID userId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        RagChatRequest request = new RagChatRequest();
        request.setRequestId(requestId);
        Message<RagChatRequest> message = new GenericMessage<>(request);

        controller.handleMessageException(
                org.mockito.Mockito.mock(org.springframework.web.bind.MethodArgumentNotValidException.class),
                authentication(userId),
                message);

        ArgumentCaptor<RagChatSocketEvent> eventCaptor = ArgumentCaptor.forClass(RagChatSocketEvent.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq(userId.toString()), eq("/queue/ai-chat"), eventCaptor.capture());
        assertThat(eventCaptor.getValue().type()).isEqualTo("error");
        assertThat(eventCaptor.getValue().requestId()).isEqualTo(requestId);
    }

    private UsernamePasswordAuthenticationToken authentication(UUID userId) {
        return new UsernamePasswordAuthenticationToken(userId, null);
    }
}
