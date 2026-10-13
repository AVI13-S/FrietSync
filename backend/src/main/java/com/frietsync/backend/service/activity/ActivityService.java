package com.frietsync.backend.service.activity;

import com.frietsync.backend.dto.activity.ActivityResponse;
import com.frietsync.backend.dto.activity.PresenceResponse;
import com.frietsync.backend.dto.activity.ProjectSummaryResponse;
import com.frietsync.backend.entity.issue.IssueHistoryAction;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ActivityService {
    List<PresenceResponse> presence(UUID projectId, UUID userId);

    List<ActivityResponse> activity(UUID projectId, int limit, UUID userId);

    List<ActivityResponse> events(UUID projectId, IssueHistoryAction action, Instant from, Instant to,
                                  int limit, UUID userId);

    ProjectSummaryResponse summary(UUID projectId, UUID userId);

    byte[] summaryPdf(UUID projectId, UUID userId);
}