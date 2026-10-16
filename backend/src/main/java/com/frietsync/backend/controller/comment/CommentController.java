package com.frietsync.backend.controller.comment;

import com.frietsync.backend.dto.issue.CreateIssueCommentRequest;
import com.frietsync.backend.dto.issue.IssueCommentResponse;
import com.frietsync.backend.dto.sprint.CreateSprintCommentRequest;
import com.frietsync.backend.dto.sprint.SprintCommentResponse;
import com.frietsync.backend.service.comment.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/issues/{issueId}/comments")
    public ResponseEntity<IssueCommentResponse> addIssueComment(
            @PathVariable UUID issueId,
            @Valid @RequestBody CreateIssueCommentRequest request,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.addIssueComment(issueId, request, userId));
    }

    @GetMapping("/issues/{issueId}/comments")
    public ResponseEntity<List<IssueCommentResponse>> listIssueComments(
            @PathVariable UUID issueId,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(commentService.listIssueComments(issueId, userId));
    }

    @PostMapping("/sprints/{sprintId}/comments")
    public ResponseEntity<SprintCommentResponse> addSprintComment(
            @PathVariable UUID sprintId,
            @Valid @RequestBody CreateSprintCommentRequest request,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.addSprintComment(sprintId, request, userId));
    }

    @GetMapping("/sprints/{sprintId}/comments")
    public ResponseEntity<List<SprintCommentResponse>> listSprintComments(
            @PathVariable UUID sprintId,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(commentService.listSprintComments(sprintId, userId));
    }
}
