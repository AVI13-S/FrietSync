package com.frietsync.backend.controller.canvas;

import com.frietsync.backend.dto.canvas.CanvasDetailResponse;
import com.frietsync.backend.dto.canvas.CanvasResponse;
import com.frietsync.backend.dto.canvas.CanvasTemplateResponse;
import com.frietsync.backend.service.canvas.CanvasService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CanvasController {

    private final CanvasService canvasService;

    @GetMapping("/canvas-templates")
    public ResponseEntity<List<CanvasTemplateResponse>> templates() {
        return ResponseEntity.ok(canvasService.templates());
    }

    @GetMapping("/projects/{projectId}/canvases")
    public ResponseEntity<List<CanvasResponse>> list(@PathVariable UUID projectId,
                                                     @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(canvasService.listCanvases(projectId, userId));
    }

    @GetMapping("/canvases/{canvasId}")
    public ResponseEntity<CanvasDetailResponse> get(@PathVariable UUID canvasId,
                                                    @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(canvasService.getCanvas(canvasId, userId));
    }
}