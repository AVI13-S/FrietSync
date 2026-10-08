package com.frietsync.backend.controller.project;

import com.frietsync.backend.dto.project.*;
import com.frietsync.backend.service.project.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody ProjectRequest request,
                                                  @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.create(request, userId));
    }

    @GetMapping("/me")
    public ResponseEntity<List<ProjectResponse>> myProjects(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(projectService.myProjects(userId));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> get(@PathVariable UUID projectId,
                                               @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(projectService.get(projectId, userId));
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> update(@PathVariable UUID projectId,
                                                  @Valid @RequestBody ProjectRequest request,
                                                  @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(projectService.update(projectId, request, userId));
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> delete(@PathVariable UUID projectId,
                                       @AuthenticationPrincipal UUID userId) {
        projectService.delete(projectId, userId);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/{projectId}/members")
    public ResponseEntity<List<MemberResponse>> members(@PathVariable UUID projectId,
                                                        @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(projectService.getMembers(projectId, userId));
    }

    @PostMapping("/{projectId}/members")
    public ResponseEntity<MemberResponse> addMember(@PathVariable UUID projectId,
                                                    @Valid @RequestBody MemberRequest request,
                                                    @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.addMember(projectId, request, userId));
    }

    @DeleteMapping("/{projectId}/members/{memberUserId}")
    public ResponseEntity<Void> removeMember(@PathVariable UUID projectId,
                                             @PathVariable UUID memberUserId,
                                             @AuthenticationPrincipal UUID userId) {
        projectService.removeMember(projectId, memberUserId, userId);
        return ResponseEntity.noContent().build();
    }
    @PutMapping("/{projectId}/roles")
    public ResponseEntity<MemberResponse> assignRole(@PathVariable UUID projectId,
                                                     @Valid @RequestBody MemberRequest request,
                                                     @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(projectService.assignRole(projectId, request, userId));
    }

    @PostMapping("/{projectId}/labels")
    public ResponseEntity<LabelResponse> createLabel(@PathVariable UUID projectId,
                                                     @Valid @RequestBody LabelRequest request,
                                                     @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.createLabel(projectId, request, userId));
    }

    @GetMapping("/{projectId}/labels")
    public ResponseEntity<List<LabelResponse>> labels(@PathVariable UUID projectId,
                                                      @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(projectService.getLabels(projectId, userId));
    }
}