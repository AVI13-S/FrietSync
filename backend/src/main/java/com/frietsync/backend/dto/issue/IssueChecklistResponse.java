package com.frietsync.backend.dto.issue;

import com.frietsync.backend.entity.issue.IssueStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class IssueChecklistResponse {

    private UUID projectId;

    private int totalIssues;

    private int completedCount;

    private int inReviewCount;

    private int remainingCount;

    private int completionPercent;

    private Map<IssueStatus, Long> statusCounts;

    private int overdueCount;
    private int dueSoonCount;
    private int unassignedCount;
    private int blockedCount;
    private int highPriorityRemainingCount;
    private int reopenedCount;
    private int assignedToMeRemainingCount;
    private int pendingAssignmentRequests;

    private List<IssueChecklistItemResponse> completed;
    private List<IssueChecklistItemResponse> inReview;
    private List<IssueChecklistItemResponse> remaining;
}