package com.frietsync.backend.dto.briefing;

import com.frietsync.backend.dto.issue.IssueSummaryResponse;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class DailyBriefingResponse {

    private LocalDate date;
    private String timeZone;
    private String userName;
    private String headline;

    private int openIssues;
    private int completedIssues;

    private List<IssueSummaryResponse> overdue;
    private List<IssueSummaryResponse> dueToday;
    private List<IssueSummaryResponse> dueSoon;
    private Long unreadNotifications;
}