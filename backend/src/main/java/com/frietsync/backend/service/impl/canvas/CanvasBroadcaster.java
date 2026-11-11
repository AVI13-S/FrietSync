package com.frietsync.backend.service.impl.canvas;

import com.frietsync.backend.dto.canvas.CanvasEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class CanvasBroadcaster {

    public static final String TOPIC_PREFIX = "/topic/canvases/";

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCanvasEvent(CanvasEvent event) {
        try {
            SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create();
            headers.setHeader("canvasProjectId", event.getProjectId());
            headers.setLeaveMutable(true);
            messagingTemplate.convertAndSend(
                    TOPIC_PREFIX + event.getCanvasId(), event, headers.getMessageHeaders());
        } catch (RuntimeException e) {

            log.warn("Could not broadcast canvas event {} for canvas {}",
                    event.getType(), event.getCanvasId(), e);
        }
    }
}