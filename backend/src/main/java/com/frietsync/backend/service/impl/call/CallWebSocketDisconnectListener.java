package com.frietsync.backend.service.impl.call;

import com.frietsync.backend.repository.call.CallParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CallWebSocketDisconnectListener implements ApplicationListener<SessionDisconnectEvent> {

    private final CallParticipantRepository callParticipantRepository;

    @Override
    public void onApplicationEvent(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        if (principal != null) {
            try {
                UUID userId = UUID.fromString(principal.getName());
                int updatedCount = callParticipantRepository.markDisconnectedForUser(userId);
                if (updatedCount > 0) {
                    log.debug("Marked {} active call participants as disconnected for user {}", updatedCount, userId);
                }
            } catch (Exception e) {
                log.error("Failed to process WebSocket disconnect for user session", e);
            }
        }
    }
}
