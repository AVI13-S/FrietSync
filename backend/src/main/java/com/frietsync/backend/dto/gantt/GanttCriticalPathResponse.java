package com.frietsync.backend.dto.gantt;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class GanttCriticalPathResponse {

    private List<UUID> issueIds = new ArrayList<>();
    private List<GanttItemResponse> issues = new ArrayList<>();
    private LocalDate projectedEndDate;
    private LocalDate projectDeadline;
    private int daysLate;
}