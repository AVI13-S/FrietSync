package com.frietsync.backend.dto.activity;

import com.frietsync.backend.entity.project.ProjectStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ProjectSummaryResponse {

    private UUID projectId;
    private String projectName;
    private ProjectStatus projectStatus;
    private Instant generatedAt;

    private String summaryText;

    private LocalDate startDate;
    private LocalDate deadline;
    private LocalDate completedDate;
    private Long plannedDurationDays;
    private Long actualDurationDays;
    private boolean projectDelayed;
    private long projectDaysLate;

    private int totalIssues;
    private int openIssues;
    private int inProgressIssues;
    private int resolvedIssues;
    private int reopenedIssues;
    private int closedIssues;

    private int overdueIssues;
    private int completedLateIssues;
    private List<DelayedIssueResponse> delayedIssues;

    private double totalEstimatedHours;
    private double finishedEstimatedHours;
    private double finishedActualHours;

    private int totalSprints;
    private int plannedSprints;
    private int activeSprints;
    private int completedSprints;
    private int overdueSprints;
}