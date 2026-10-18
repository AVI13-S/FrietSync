package com.frietsync.backend.service.impl.notification;

import com.frietsync.backend.service.notification.NotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationWebSocketListener {

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void sendToUser(NotificationEvent event) {
        if (event.notification() == null) {
            return;
        }
        messagingTemplate.convertAndSendToUser(
                event.userId().toString(),
                "/queue/notifications",
                event.notification()
        );
    }
}
