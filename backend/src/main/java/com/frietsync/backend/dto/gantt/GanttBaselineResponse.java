package com.frietsync.backend.dto.gantt;

import lombok.Data;

import java.time.Instant;

@Data
public class GanttBaselineResponse {

    private Instant capturedAt;
    private int issueCount;
    private int sprintCount;
}