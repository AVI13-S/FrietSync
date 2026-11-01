package com.frietsync.backend.controller.issue;

import com.frietsync.backend.dto.issue.IssueChecklistResponse;
import com.frietsync.backend.service.issue.IssueChecklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class IssueChecklistController {

    private final IssueChecklistService checklistService;

    @GetMapping("/projects/{projectId}/issues/checklist")
    public ResponseEntity<IssueChecklistResponse> checklist(@PathVariable UUID projectId,
                                                            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(checklistService.checklist(projectId, userId));
    }
}