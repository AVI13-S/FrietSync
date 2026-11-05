package com.frietsync.backend.controller.gantt;

import com.frietsync.backend.dto.gantt.GanttBaselineResponse;
import com.frietsync.backend.dto.gantt.GanttCriticalPathResponse;
import com.frietsync.backend.dto.gantt.GanttMoveRequest;
import com.frietsync.backend.dto.gantt.GanttMoveResponse;
import com.frietsync.backend.dto.gantt.GanttResponse;
import com.frietsync.backend.service.gantt.GanttService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/gantt")
@RequiredArgsConstructor
public class GanttController {

    private final GanttService ganttService;

    @GetMapping
    public GanttResponse getGantt(@PathVariable UUID projectId, Authentication authentication) {
        return ganttService.getGantt(projectId, currentUserId(authentication));
    }

    @GetMapping("/critical-path")
    public GanttCriticalPathResponse getCriticalPath(@PathVariable UUID projectId,
                                                     Authentication authentication) {
        return ganttService.getCriticalPath(projectId, currentUserId(authentication));
    }

    @PatchMapping("/issues/{issueId}")
    public GanttMoveResponse moveIssue(@PathVariable UUID projectId,
                                       @PathVariable UUID issueId,
                                       @Valid @RequestBody GanttMoveRequest request,
                                       Authentication authentication) {
        return ganttService.moveIssue(projectId, issueId, request, currentUserId(authentication));
    }

    @PatchMapping("/sprints/{sprintId}")
    public GanttMoveResponse moveSprint(@PathVariable UUID projectId,
                                        @PathVariable UUID sprintId,
                                        @Valid @RequestBody GanttMoveRequest request,
                                        Authentication authentication) {
        return ganttService.moveSprint(projectId, sprintId, request, currentUserId(authentication));
    }

    @PostMapping("/baseline")
    @ResponseStatus(HttpStatus.CREATED)
    public GanttBaselineResponse setBaseline(@PathVariable UUID projectId, Authentication authentication) {
        return ganttService.setBaseline(projectId, currentUserId(authentication));
    }

    @DeleteMapping("/baseline")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearBaseline(@PathVariable UUID projectId, Authentication authentication) {
        ganttService.clearBaseline(projectId, currentUserId(authentication));
    }

    private UUID currentUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}