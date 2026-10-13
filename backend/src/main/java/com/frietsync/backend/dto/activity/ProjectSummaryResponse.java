package com.frietsync.backend.dto.activity;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ProjectSummaryResponse {

    private UUID projectId;

    private int totalIssues;
    private int openIssues;
    private int inProgressIssues;
    private int resolvedIssues;
    private int reopenedIssues;
    private int closedIssues;
    private double completionPercent;

    private double estimatedHoursTotal;
    private double estimatedHoursCompleted;
    private double estimatedHoursRemaining;

    private Double averageDaysToResolve;

    private int overdueIssues;
    private List<DelayedIssueResponse> delayedIssues;
}