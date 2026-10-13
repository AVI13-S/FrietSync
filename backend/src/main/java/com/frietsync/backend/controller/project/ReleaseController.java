package com.frietsync.backend.controller.project;

import com.frietsync.backend.dto.project.AttachIssuesRequest;
import com.frietsync.backend.dto.project.ReleaseRequest;
import com.frietsync.backend.dto.project.ReleaseResponse;
import com.frietsync.backend.service.project.ReleaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReleaseController {
    private final ReleaseService releaseService;

    @PostMapping("/projects/{projectId}/releases")
    public ResponseEntity<ReleaseResponse> create(@PathVariable UUID projectId,
            @Valid @RequestBody ReleaseRequest request, @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(releaseService.create(projectId, userId, request));
    }

    @GetMapping("/projects/{projectId}/releases")
    public List<ReleaseResponse> list(@PathVariable UUID projectId,
            @AuthenticationPrincipal UUID userId) {
        return releaseService.list(projectId, userId);
    }

    @PatchMapping("/releases/{releaseId}")
    public ReleaseResponse update(@PathVariable UUID releaseId, @Valid @RequestBody ReleaseRequest request,
            @AuthenticationPrincipal UUID userId) {
        return releaseService.update(releaseId, userId, request);
    }

    @DeleteMapping("/releases/{releaseId}")
    public ResponseEntity<Void> delete(@PathVariable UUID releaseId,
            @AuthenticationPrincipal UUID userId) {
        releaseService.delete(releaseId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/releases/{releaseId}/issues")
    public ReleaseResponse attachIssues(@PathVariable UUID releaseId,
            @Valid @RequestBody AttachIssuesRequest request, @AuthenticationPrincipal UUID userId) {
        return releaseService.attachIssues(releaseId, userId, request);
    }
}
