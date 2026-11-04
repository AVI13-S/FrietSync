package com.frietsync.backend.dto.gantt;

import com.frietsync.backend.dto.issue.IssueAssigneeResponse;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class GanttItemResponse {

    private UUID id;
    private String type;
    private String name;
    private Integer issueNumber;
    private String status;
    private String priority;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate baselineStartDate;
    private LocalDate baselineEndDate;
    private int delayDays;
    private boolean overdue;
    private boolean critical;
    private boolean atRisk;
    private UUID milestoneId;
    private UUID linkedIssueId;
    private List<IssueAssigneeResponse> assignees = new ArrayList<>();
}