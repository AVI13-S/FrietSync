package com.frietsync.backend.controller.activity;

import com.frietsync.backend.dto.activity.ActivityResponse;
import com.frietsync.backend.dto.activity.PresenceResponse;
import com.frietsync.backend.dto.activity.ProjectSummaryResponse;
import com.frietsync.backend.entity.issue.IssueHistoryAction;
import com.frietsync.backend.service.activity.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping("/projects/{projectId}/presence")
    public ResponseEntity<List<PresenceResponse>> presence(@PathVariable UUID projectId,
                                                           @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(activityService.presence(projectId, userId));
    }

    @GetMapping("/projects/{projectId}/activity")
    public ResponseEntity<List<ActivityResponse>> activity(@PathVariable UUID projectId,
                                                           @RequestParam(defaultValue = "50") int limit,
                                                           @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(activityService.activity(projectId, limit, userId));
    }

    @GetMapping("/projects/{projectId}/events")
    public ResponseEntity<List<ActivityResponse>> events(@PathVariable UUID projectId,
                                                         @RequestParam(required = false) IssueHistoryAction action,
                                                         @RequestParam(required = false) Instant from,
                                                         @RequestParam(required = false) Instant to,
                                                         @RequestParam(defaultValue = "200") int limit,
                                                         @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(activityService.events(projectId, action, from, to, limit, userId));
    }

    @GetMapping("/projects/{projectId}/summary")
    public ResponseEntity<ProjectSummaryResponse> summary(@PathVariable UUID projectId,
                                                          @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(activityService.summary(projectId, userId));
    }

    @GetMapping("/projects/{projectId}/summary/pdf")
    public ResponseEntity<byte[]> summaryPdf(@PathVariable UUID projectId,
                                             @AuthenticationPrincipal UUID userId) {
        byte[] pdf = activityService.summaryPdf(projectId, userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("project-summary.pdf").build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}