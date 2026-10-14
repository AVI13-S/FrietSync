package com.frietsync.backend.dto.replay;

import com.frietsync.backend.dto.issue.IssueSummaryResponse;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ReplaySnapshotResponse {

    private UUID projectId;
    private Instant at;

    private int totalIssues;
    private int openIssues;
    private int inProgressIssues;
    private int resolvedIssues;
    private int reopenedIssues;
    private int closedIssues;
    private double completionPercent;

    private int overdueIssues;
    private List<UUID> overdueIssueIds;

    private List<IssueSummaryResponse> issues;
}