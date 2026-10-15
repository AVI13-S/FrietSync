package com.frietsync.backend.service.issue;

import com.frietsync.backend.dto.issue.DependencyGraphResponse;
import com.frietsync.backend.dto.issue.IssueImpactResponse;
import com.frietsync.backend.dto.issue.IssueLinkRequest;
import com.frietsync.backend.dto.issue.IssueLinkResponse;
import com.frietsync.backend.dto.issue.IssueSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface IssueLinkService {
    IssueLinkResponse create(UUID issueId, IssueLinkRequest request, UUID userId);

    List<IssueLinkResponse> list(UUID issueId, UUID userId);

    void delete(UUID linkId, UUID userId);

    IssueImpactResponse impact(UUID issueId, UUID userId);

    List<IssueSummaryResponse> dependencies(UUID issueId, UUID userId);

    DependencyGraphResponse dependencyGraph(UUID projectId, UUID userId);
}