package com.frietsync.backend.config.issue;

import com.frietsync.backend.service.impl.project.ProjectAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Pattern ISSUE_HISTORY_TOPIC =
            Pattern.compile("^/topic/issues/([0-9a-fA-F\\-]{36})/history$");

    private final ProjectAccess access;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (accessor.getCommand() == StompCommand.CONNECT) {
            String header = accessor.getFirstNativeHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ")) {
                throw new MessagingException("Missing token");
            }
            UUID userId = userIdFromToken(header.substring(7));
            accessor.setUser(new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of()));
        }

        if (accessor.getCommand() == StompCommand.SUBSCRIBE) {
            Principal user = accessor.getUser();
            if (user == null) {
                throw new MessagingException("Not authenticated");
            }
            Matcher m = ISSUE_HISTORY_TOPIC.matcher(String.valueOf(accessor.getDestination()));
            if (!m.matches()) {
                throw new MessagingException("Subscription not allowed");
            }
            UUID issueId = UUID.fromString(m.group(1));
            UUID userId = UUID.fromString(user.getName());
            var issue = access.issue(issueId);
            access.requireMember(access.project(issue.getProjectId()), userId);
        }
        return message;
    }

    private UUID userIdFromToken(String token) {
        throw new MessagingException("JWT validation not wired yet");
    }
}