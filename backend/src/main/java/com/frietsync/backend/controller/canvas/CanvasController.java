package com.frietsync.backend.controller.canvas;

import com.frietsync.backend.dto.canvas.CanvasDetailResponse;
import com.frietsync.backend.dto.canvas.CanvasResponse;
import com.frietsync.backend.dto.canvas.CreateCanvasRequest;
import com.frietsync.backend.dto.canvas.RenameCanvasRequest;
import com.frietsync.backend.service.canvas.CanvasService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
public class CanvasController {

    private final CanvasService canvasService;

    @PostMapping("/projects/{projectId}/canvases")
    public ResponseEntity<CanvasDetailResponse> create(@PathVariable UUID projectId,
                                                       @RequestBody CreateCanvasRequest request,
                                                       @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(canvasService.createCanvas(projectId, request, userId));
    }

    @GetMapping("/projects/{projectId}/canvases")
    public ResponseEntity<List<CanvasResponse>> list(@PathVariable UUID projectId,
                                                     @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(canvasService.listCanvases(projectId, userId));
    }

    @PatchMapping("/canvases/{canvasId}")
    public ResponseEntity<CanvasResponse> rename(@PathVariable UUID canvasId,
                                                 @RequestBody RenameCanvasRequest request,
                                                 @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(canvasService.renameCanvas(canvasId, request, userId));
    }

    @GetMapping("/canvases/{canvasId}")
    public ResponseEntity<CanvasDetailResponse> get(@PathVariable UUID canvasId,
                                                    @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(canvasService.getCanvas(canvasId, userId));
    }

    @DeleteMapping("/canvases/{canvasId}")
    public ResponseEntity<Void> delete(@PathVariable UUID canvasId,
                                       @AuthenticationPrincipal UUID userId) {
        canvasService.deleteCanvas(canvasId, userId);
        return ResponseEntity.noContent().build();
    }
}