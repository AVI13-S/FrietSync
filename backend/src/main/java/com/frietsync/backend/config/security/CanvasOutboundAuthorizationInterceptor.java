package com.frietsync.backend.config.security;

import com.frietsync.backend.exception.ForbiddenException;
import com.frietsync.backend.exception.ResourceNotFoundException;
import com.frietsync.backend.service.impl.project.ProjectAccess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.user.SimpSession;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CanvasOutboundAuthorizationInterceptor implements ChannelInterceptor {

    private static final String CANVAS_TOPIC_PREFIX = "/topic/canvases/";
    private static final String PROJECT_ID_HEADER = "canvasProjectId";

    private final ProjectAccess access;
    private final ObjectProvider<SimpUserRegistry> userRegistry;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        SimpMessageHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, SimpMessageHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        String destination = accessor.getDestination();
        if (accessor.getMessageType() != SimpMessageType.MESSAGE
                || destination == null || !destination.startsWith(CANVAS_TOPIC_PREFIX)) {
            return message;
        }

        UUID userId = userId(accessor);
        UUID projectId = projectId(accessor.getHeader(PROJECT_ID_HEADER));
        if (userId == null || projectId == null) {
            log.warn("Dropping canvas event with missing authorization context for {}", destination);
            return null;
        }

        try {
            access.requireMember(access.project(projectId), userId);
            return message;
        } catch (ForbiddenException | ResourceNotFoundException ex) {
            log.debug("Dropping canvas event for user {} without current project access", userId);
            return null;
        }
    }

    private UUID projectId(Object value) {
        if (value instanceof UUID id) {
            return id;
        }
        if (value instanceof String text) {
            try {
                return UUID.fromString(text);
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
        return null;
    }

    private UUID userId(SimpMessageHeaderAccessor accessor) {
        UUID userId = StompPrincipals.userId(accessor.getUser());
        if (userId != null) {
            return userId;
        }

        String sessionId = accessor.getSessionId();
        if (sessionId == null) {
            return null;
        }
        for (SimpUser user : userRegistry.getObject().getUsers()) {
            for (SimpSession session : user.getSessions()) {
                if (sessionId.equals(session.getId())) {
                    try {
                        return UUID.fromString(user.getName());
                    } catch (IllegalArgumentException ex) {
                        return null;
                    }
                }
            }
        }
        return null;
    }
}
