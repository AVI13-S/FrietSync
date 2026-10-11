package com.frietsync.backend.controller.poll;

import com.frietsync.backend.dto.poll.ClosePollRequest;
import com.frietsync.backend.dto.poll.CreatePollRequest;
import com.frietsync.backend.dto.poll.PollResponse;
import com.frietsync.backend.dto.poll.VoteRequest;
import com.frietsync.backend.service.poll.PollService;
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
public class PollController {
    private final PollService pollService;

    @PostMapping("/projects/{projectId}/polls")
    public ResponseEntity<PollResponse> create(@PathVariable UUID projectId,
            @Valid @RequestBody CreatePollRequest request, @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pollService.create(projectId, request, userId));
    }

    @GetMapping("/projects/{projectId}/polls")
    public ResponseEntity<List<PollResponse>> list(@PathVariable UUID projectId,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(pollService.list(projectId, userId));
    }

    @GetMapping("/polls/{pollId}")
    public ResponseEntity<PollResponse> get(@PathVariable UUID pollId,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(pollService.get(pollId, userId));
    }

    @PutMapping("/polls/{pollId}/votes")
    public ResponseEntity<PollResponse> vote(@PathVariable UUID pollId,
            @Valid @RequestBody VoteRequest request, @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(pollService.vote(pollId, request, userId));
    }

    @PostMapping("/polls/{pollId}/close")
    public ResponseEntity<PollResponse> close(@PathVariable UUID pollId,
            @Valid @RequestBody ClosePollRequest request, @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(pollService.close(pollId, request.getDecisionRecord(), userId));
    }
}
