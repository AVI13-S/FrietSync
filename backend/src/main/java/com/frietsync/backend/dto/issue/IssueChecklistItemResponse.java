package com.frietsync.backend.dto.issue;

import com.frietsync.backend.entity.issue.IssuePriority;
import com.frietsync.backend.entity.issue.IssueStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class IssueChecklistItemResponse {

    private UUID id;
    private int issueNumber;
    private String title;
    private IssueStatus status;
    private IssuePriority priority;
    private LocalDate dueDate;

    private boolean checked;

    private boolean overdue;

    private boolean dueSoon;

    private boolean unassigned;

    private boolean blocked;

    private List<Integer> blockedByIssueNumbers;

    private List<IssueAssigneeResponse> assignees;
    private Instant resolvedAt;
    private Instant closedAt;
}