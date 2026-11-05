package com.frietsync.backend.config.security;

import com.frietsync.backend.entity.canvas.Canvas;
import com.frietsync.backend.entity.issue.Issue;
import com.frietsync.backend.repository.canvas.CanvasRepository;
import com.frietsync.backend.service.impl.project.ProjectAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RealtimeTopicGuard implements ChannelInterceptor {

    private static final String ISSUE_PREFIX = "/topic/issues/";
    private static final String ISSUE_SUFFIX = "/history";
    private static final String CANVAS_PREFIX = "/topic/canvases/";

    private final ProjectAccess access;
    private final CanvasRepository canvasRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.SUBSCRIBE) {
            return message;
        }
        String destination = accessor.getDestination();
        if (destination == null) {
            return message;
        }

        UUID issueId = issueIdFromTopic(destination);
        UUID canvasId = canvasIdFromTopic(destination);
        if (issueId == null && canvasId == null) {
            return message;
        }

        UUID userId = StompPrincipals.userId(accessor.getUser());
        if (userId == null) {
            throw new MessagingException("Not authenticated");
        }

        if (issueId != null) {
            Issue issue = access.issue(issueId);
            access.requireMember(access.project(issue.getProjectId()), userId);
        } else {
            Canvas canvas = canvasRepository.findById(canvasId).orElse(null);
            if (canvas == null) {
                throw new MessagingException("Canvas not found");
            }
            access.requireMember(access.project(canvas.getProjectId()), userId);
        }
        return message;
    }

    private UUID issueIdFromTopic(String destination) {
        if (!destination.startsWith(ISSUE_PREFIX) || !destination.endsWith(ISSUE_SUFFIX)) {
            return null;
        }
        if (destination.length() <= ISSUE_PREFIX.length() + ISSUE_SUFFIX.length()) {
            return null;
        }
        String idText = destination.substring(ISSUE_PREFIX.length(),
                destination.length() - ISSUE_SUFFIX.length());
        return toUuid(idText);
    }

    private UUID canvasIdFromTopic(String destination) {
        if (!destination.startsWith(CANVAS_PREFIX)) {
            return null;
        }
        return toUuid(destination.substring(CANVAS_PREFIX.length()));
    }

    private UUID toUuid(String text) {
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}