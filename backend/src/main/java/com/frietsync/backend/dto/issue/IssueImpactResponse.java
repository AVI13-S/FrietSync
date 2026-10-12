package com.frietsync.backend.dto.issue;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class IssueImpactResponse {

    private IssueSummaryResponse issue;

    private List<IssueSummaryResponse> blockedBy;

    private List<IssueSummaryResponse> directDependents;

    private List<IssueSummaryResponse> indirectDependents;

    private List<IssueSummaryResponse> related;

    private int totalAffected;
}