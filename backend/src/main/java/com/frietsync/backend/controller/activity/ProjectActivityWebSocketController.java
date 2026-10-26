package com.frietsync.backend.controller.activity;

import com.frietsync.backend.dto.activity.ActivityResponse;
import com.frietsync.backend.service.activity.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ProjectActivityWebSocketController {

    private final ActivityService activityService;

    @SubscribeMapping("/projects/{projectId}/activity")
    public List<ActivityResponse> recentActivity(@DestinationVariable UUID projectId,
                                                  Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return activityService.recentActivity(projectId, userId);
    }
}
