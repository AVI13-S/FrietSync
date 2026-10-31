package com.frietsync.backend.service.issue;

import com.frietsync.backend.dto.issue.IssueChecklistResponse;

import java.util.UUID;

public interface IssueChecklistService {
    IssueChecklistResponse checklist(UUID projectId, UUID userId);
}