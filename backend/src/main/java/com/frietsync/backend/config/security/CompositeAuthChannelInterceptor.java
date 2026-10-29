package com.frietsync.backend.config.security;

import com.frietsync.backend.entity.user.User;
import com.frietsync.backend.repository.user.UserRepository;
import com.frietsync.backend.repository.poll.PollRepository;
import com.frietsync.backend.service.chat.ChatService;
import com.frietsync.backend.service.community.CommunityService;
import com.frietsync.backend.service.impl.project.ProjectAccess;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class CompositeAuthChannelInterceptor implements ChannelInterceptor {

    private static final String SEND_PREFIX = "/app/projects/";
    private static final String CHAT_SUFFIX = "/chat";
    private static final String TOPIC_PREFIX = "/topic/projects/";
    private static final String CALLS_SUFFIX = "/calls";
    private static final String ACTIVITY_SUFFIX = "/activity";
    private static final String APP_PROJECT_PREFIX = "/app/projects/";
    private static final String NOTIFICATION_DESTINATION = "/user/queue/notifications";
    private static final String COMMUNITY_TOPIC_PREFIX = "/topic/workspaces/";
    private static final String COMMUNITY_TOPIC_SUFFIX = "/community";

    private static final Pattern ISSUE_HISTORY_TOPIC =
            Pattern.compile("^/topic/issues/([0-9a-fA-F\\-]{36})/history$");

    private final ChatService chatService;
    private final CommunityService communityService;
    private final ProjectAccess projectAccess;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PollRepository pollRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (accessor.getCommand() == StompCommand.CONNECT) {
            authenticate(accessor);
        } else if (accessor.getCommand() == StompCommand.SUBSCRIBE) {
            String destination = accessor.getDestination();
            if (destination != null && ISSUE_HISTORY_TOPIC.matcher(destination).matches()) {
                authenticateIssueSubscription(accessor, destination);
            } else {
                authenticateChatOrProjectSubscription(accessor, destination);
            }
        } else if (accessor.getCommand() == StompCommand.SEND) {
            String destination = accessor.getDestination();
            if (destination != null && destination.startsWith(SEND_PREFIX) && destination.endsWith(CHAT_SUFFIX)) {
                chatService.assertProjectAccess(
                        projectId(destination, SEND_PREFIX, CHAT_SUFFIX),
                        userId(accessor)
                );
            }
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            String authHeaderLowerCase = accessor.getFirstNativeHeader("authorization");
            if (authHeaderLowerCase != null && authHeaderLowerCase.startsWith("Bearer ")) {
                authorization = authHeaderLowerCase;
            } else {
                throw new AccessDeniedException("A bearer access token is required to connect");
            }
        }

        Claims claims;
        UUID userId;
        try {
            claims = jwtUtil.validateAndParse(authorization.substring(7));
            userId = UUID.fromString(claims.getSubject());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new AccessDeniedException("Invalid or expired bearer access token", ex);
        }

        User user = userRepository.findById(userId).orElse(null);
        Integer tokenPasswordVersion = claims.get("passwordVersion", Integer.class);
        if (user == null || !user.isActive() || tokenPasswordVersion == null
                || !tokenPasswordVersion.equals(user.getPasswordVersion())) {
            throw new AccessDeniedException("The user or access token is no longer valid");
        }

        accessor.setUser(new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        ));
    }

    private void authenticateIssueSubscription(StompHeaderAccessor accessor, String destination) {
        UUID userId = userId(accessor);
        Matcher m = ISSUE_HISTORY_TOPIC.matcher(destination);
        if (m.matches()) {
            UUID issueId = UUID.fromString(m.group(1));
            var issue = projectAccess.issue(issueId);
            projectAccess.requireMember(projectAccess.project(issue.getProjectId()), userId);
        }
    }

    private void authenticateChatOrProjectSubscription(StompHeaderAccessor accessor, String destination) {
        UUID userId = userId(accessor);
        if (isCommunityTopic(destination)) {
            communityService.assertWorkspaceMember(communityWorkspaceId(destination), userId);
        } else if (!NOTIFICATION_DESTINATION.equals(destination)) {
            chatService.assertProjectAccess(subscriptionProjectId(destination), userId);
        }
    }

    private boolean isCommunityTopic(String destination) {
        return destination != null
                && destination.startsWith(COMMUNITY_TOPIC_PREFIX)
                && destination.endsWith(COMMUNITY_TOPIC_SUFFIX)
                && destination.length() > COMMUNITY_TOPIC_PREFIX.length() + COMMUNITY_TOPIC_SUFFIX.length();
    }

    private UUID communityWorkspaceId(String destination) {
        String value = destination.substring(COMMUNITY_TOPIC_PREFIX.length(),
                destination.length() - COMMUNITY_TOPIC_SUFFIX.length());
        try {
            if (value.isBlank() || value.contains("/")) {
                throw new IllegalArgumentException("Invalid workspace identifier");
            }
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new AccessDeniedException("Invalid workspace identifier", ex);
        }
    }

    private UUID userId(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof UsernamePasswordAuthenticationToken authentication
                && authentication.getPrincipal() instanceof UUID id) {
            return id;
        }
        Principal principal = accessor.getUser();
        if (principal != null) {
            try {
                return UUID.fromString(principal.getName());
            } catch (Exception ignored) {}
        }
        throw new AccessDeniedException("A valid authenticated connection is required");
    }

    private UUID subscriptionProjectId(String destination) {
        if (destination != null && destination.startsWith(TOPIC_PREFIX)
                && destination.endsWith(CALLS_SUFFIX)) {
            return topicProjectId(destination, CALLS_SUFFIX);
        }
        if (destination != null && destination.startsWith(TOPIC_PREFIX)
                && destination.endsWith(ACTIVITY_SUFFIX)) {
            return topicProjectId(destination, ACTIVITY_SUFFIX);
        }
        if (destination != null && destination.startsWith(APP_PROJECT_PREFIX)
                && destination.endsWith(ACTIVITY_SUFFIX)) {
            return projectId(destination, APP_PROJECT_PREFIX, ACTIVITY_SUFFIX);
        }
        String pollResultsPrefix = "/topic/polls/";
        String pollResultsSuffix = "/results";
        if (destination != null && destination.startsWith(pollResultsPrefix)
                && destination.endsWith(pollResultsSuffix)) {
            String value = destination.substring(pollResultsPrefix.length(),
                    destination.length() - pollResultsSuffix.length());
            try {
                if (value.isBlank() || value.contains("/")) throw new IllegalArgumentException();
                UUID pollId = UUID.fromString(value);
                return pollRepository.findById(pollId)
                        .map(poll -> poll.getProjectId())
                        .orElseThrow(() -> new AccessDeniedException("Poll not found"));
            } catch (IllegalArgumentException ex) {
                throw new AccessDeniedException("Invalid poll identifier", ex);
            }
        }
        if (destination != null && destination.startsWith(TOPIC_PREFIX) && destination.endsWith("/polls")) {
            String value = destination.substring(TOPIC_PREFIX.length(), destination.length() - "/polls".length());
            try {
                if (value.isBlank() || value.contains("/")) throw new IllegalArgumentException();
                return UUID.fromString(value);
            } catch (IllegalArgumentException ex) {
                throw new AccessDeniedException("Invalid project identifier", ex);
            }
        }
        return projectId(destination, TOPIC_PREFIX, CHAT_SUFFIX);
    }

    private UUID topicProjectId(String destination, String suffix) {
        return projectId(destination, TOPIC_PREFIX, suffix);
    }

    private UUID projectId(String destination, String prefix, String suffix) {
        if (destination == null || !destination.startsWith(prefix) || !destination.endsWith(suffix)) {
            throw new AccessDeniedException("Unsupported destination");
        }
        String value = destination.substring(prefix.length(), destination.length() - suffix.length());
        try {
            if (value.isBlank() || value.contains("/")) throw new IllegalArgumentException();
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new AccessDeniedException("Invalid project identifier", ex);
        }
    }
}
