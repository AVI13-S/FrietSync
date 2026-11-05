package com.frietsync.backend.controller.canvas;

import com.frietsync.backend.config.security.StompPrincipals;
import com.frietsync.backend.dto.canvas.CanvasDetailResponse;
import com.frietsync.backend.dto.canvas.CanvasElementRequest;
import com.frietsync.backend.dto.canvas.CanvasElementUpdateRequest;
import com.frietsync.backend.dto.canvas.ConvertToIssueRequest;
import com.frietsync.backend.dto.canvas.ConvertToIssueResponse;
import com.frietsync.backend.dto.canvas.CreateCanvasRequest;
import com.frietsync.backend.dto.canvas.RenameCanvasRequest;
import com.frietsync.backend.exception.ForbiddenException;
import com.frietsync.backend.service.canvas.CanvasService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class CanvasSocketController {

    private final CanvasService canvasService;

    @MessageMapping("/projects/{projectId}/canvases/create")
    @SendToUser(destinations = "/queue/canvas-created", broadcast = false)
    public CanvasDetailResponse createCanvas(@DestinationVariable UUID projectId,
                                             @Payload CreateCanvasRequest request,
                                             Principal principal) {
        return canvasService.createCanvas(projectId, request, userId(principal));
    }

    @MessageMapping("/canvases/{canvasId}/rename")
    public void renameCanvas(@DestinationVariable UUID canvasId,
                             @Payload RenameCanvasRequest request,
                             Principal principal) {
        canvasService.renameCanvas(canvasId, request, userId(principal));
    }

    @MessageMapping("/canvases/{canvasId}/delete")
    public void deleteCanvas(@DestinationVariable UUID canvasId, Principal principal) {
        canvasService.deleteCanvas(canvasId, userId(principal));
    }

    @MessageMapping("/canvases/{canvasId}/elements/create")
    public void createElement(@DestinationVariable UUID canvasId,
                              @Payload CanvasElementRequest request,
                              Principal principal) {
        canvasService.createElement(canvasId, request, userId(principal));
    }

    @MessageMapping("/canvases/{canvasId}/elements/{elementId}/update")
    public void updateElement(@DestinationVariable UUID canvasId,
                              @DestinationVariable UUID elementId,
                              @Payload CanvasElementUpdateRequest request,
                              Principal principal) {
        canvasService.updateElement(canvasId, elementId, request, userId(principal));
    }

    @MessageMapping("/canvases/{canvasId}/elements/{elementId}/delete")
    public void deleteElement(@DestinationVariable UUID canvasId,
                              @DestinationVariable UUID elementId,
                              Principal principal) {
        canvasService.deleteElement(canvasId, elementId, userId(principal));
    }

    @MessageMapping("/canvases/{canvasId}/elements/{elementId}/convert-to-issue")
    @SendToUser(destinations = "/queue/canvas-issue-created", broadcast = false)
    public ConvertToIssueResponse convertToIssue(@DestinationVariable UUID canvasId,
                                                 @DestinationVariable UUID elementId,
                                                 @Payload ConvertToIssueRequest request,
                                                 Principal principal) {
        return canvasService.convertToIssue(canvasId, elementId, request, userId(principal));
    }

    private UUID userId(Principal principal) {
        UUID id = StompPrincipals.userId(principal);
        if (id == null) {
            throw new ForbiddenException("Not authenticated");
        }
        return id;
    }
}