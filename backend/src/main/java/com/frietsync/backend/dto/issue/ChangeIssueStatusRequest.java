package com.frietsync.backend.dto.issue;

import com.frietsync.backend.entity.issue.IssueStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeIssueStatusRequest(@NotNull IssueStatus status) {
}
