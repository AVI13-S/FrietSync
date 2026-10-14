package com.frietsync.backend.dto.issue;

import com.frietsync.backend.entity.issue.IssueHistoryAction;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class IssueHistoryResponse {

    private UUID id;
    private UUID actorId;
    private IssueHistoryAction action;
    private String field;
    private String oldValue;
    private String newValue;
    private String comment;
    private Instant createdAt;
}