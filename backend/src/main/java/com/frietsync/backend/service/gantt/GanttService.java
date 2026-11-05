package com.frietsync.backend.service.gantt;

import com.frietsync.backend.dto.gantt.GanttBaselineResponse;
import com.frietsync.backend.dto.gantt.GanttCriticalPathResponse;
import com.frietsync.backend.dto.gantt.GanttMoveRequest;
import com.frietsync.backend.dto.gantt.GanttMoveResponse;
import com.frietsync.backend.dto.gantt.GanttResponse;

import java.util.UUID;

public interface GanttService {

    GanttResponse getGantt(UUID projectId, UUID userId);

    GanttCriticalPathResponse getCriticalPath(UUID projectId, UUID userId);

    GanttMoveResponse moveIssue(UUID projectId, UUID issueId, GanttMoveRequest request, UUID userId);

    GanttMoveResponse moveSprint(UUID projectId, UUID sprintId, GanttMoveRequest request, UUID userId);

    GanttBaselineResponse setBaseline(UUID projectId, UUID userId);

    void clearBaseline(UUID projectId, UUID userId);
}