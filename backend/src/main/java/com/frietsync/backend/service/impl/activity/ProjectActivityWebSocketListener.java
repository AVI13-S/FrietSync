package com.frietsync.backend.service.impl.activity;

import com.frietsync.backend.service.activity.ActivityService;
import com.frietsync.backend.service.activity.ProjectActivityUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ProjectActivityWebSocketListener {

    private final ActivityService activityService;
    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void broadcastLatestActivity(ProjectActivityUpdatedEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/projects/" + event.projectId() + "/activity",
                activityService.latestActivityForProject(event.projectId()));
    }
}
