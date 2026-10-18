package com.frietsync.backend.dto.issue;

import com.frietsync.backend.entity.issue.IssuePriority;
import com.frietsync.backend.entity.issue.IssueStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class IssueSummaryResponse {

    private UUID id;
    private int issueNumber;
    private String title;
    private IssueStatus status;
    private IssuePriority priority;
    private LocalDate dueDate;
}