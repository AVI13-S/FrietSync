package com.frietsync.backend.dto.gantt;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class GanttResponse {

    private UUID projectId;
    private String projectName;
    private LocalDate projectStartDate;
    private LocalDate projectDeadline;
    private LocalDate rangeStart;
    private LocalDate rangeEnd;
    private boolean hasBaseline;
    private Instant baselineCapturedAt;
    private LocalDate projectedEndDate;
    private List<UUID> criticalPath = new ArrayList<>();
    private List<GanttItemResponse> sprints = new ArrayList<>();
    private List<GanttItemResponse> issues = new ArrayList<>();
    private List<GanttItemResponse> milestones = new ArrayList<>();
    private List<GanttItemResponse> releases = new ArrayList<>();
    private List<GanttLinkResponse> links = new ArrayList<>();
}