package com.frietsync.backend.controller.issue;

import com.frietsync.backend.dto.issue.DuplicateCheckRequest;
import com.frietsync.backend.dto.issue.SimilarIssueResponse;
import com.frietsync.backend.service.impl.issue.DuplicateIssueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/issues")
@RequiredArgsConstructor
public class DuplicateIssueController {

    private final DuplicateIssueService duplicateIssueService;

    @PostMapping("/check-duplicates")
    public List<SimilarIssueResponse> checkDuplicates(
            @PathVariable UUID projectId,
            @Valid @RequestBody DuplicateCheckRequest request,
            @AuthenticationPrincipal UUID userId) {
        return duplicateIssueService.findSimilarIssues(
                projectId, request.getTitle(), request.getDescription(), userId);
    }
}