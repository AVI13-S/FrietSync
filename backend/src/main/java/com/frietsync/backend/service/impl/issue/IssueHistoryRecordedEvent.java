package com.frietsync.backend.service.impl.issue;

import com.frietsync.backend.entity.issue.IssueHistory;

public record IssueHistoryRecordedEvent(IssueHistory history) {
}