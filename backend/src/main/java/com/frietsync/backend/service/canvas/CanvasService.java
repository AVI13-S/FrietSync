package com.frietsync.backend.service.canvas;

import com.frietsync.backend.dto.canvas.CanvasDetailResponse;
import com.frietsync.backend.dto.canvas.CanvasElementRequest;
import com.frietsync.backend.dto.canvas.CanvasElementResponse;
import com.frietsync.backend.dto.canvas.CanvasElementUpdateRequest;
import com.frietsync.backend.dto.canvas.CanvasResponse;
import com.frietsync.backend.dto.canvas.CanvasTemplateResponse;
import com.frietsync.backend.dto.canvas.ConvertToIssueRequest;
import com.frietsync.backend.dto.canvas.ConvertToIssueResponse;
import com.frietsync.backend.dto.canvas.CreateCanvasRequest;
import com.frietsync.backend.dto.canvas.RenameCanvasRequest;

import java.util.List;
import java.util.UUID;

public interface CanvasService {

    List<CanvasTemplateResponse> templates();

    CanvasDetailResponse createCanvas(UUID projectId, CreateCanvasRequest request, UUID userId);

    List<CanvasResponse> listCanvases(UUID projectId, UUID userId);

    CanvasDetailResponse getCanvas(UUID canvasId, UUID userId);

    CanvasResponse renameCanvas(UUID canvasId, RenameCanvasRequest request, UUID userId);

    void deleteCanvas(UUID canvasId, UUID userId);

    CanvasElementResponse createElement(UUID canvasId, CanvasElementRequest request, UUID userId);

    CanvasElementResponse updateElement(UUID canvasId, UUID elementId,
                                        CanvasElementUpdateRequest request, UUID userId);

    void deleteElement(UUID canvasId, UUID elementId, UUID userId);

    ConvertToIssueResponse convertToIssue(UUID canvasId, UUID elementId,
                                          ConvertToIssueRequest request, UUID userId);

    void deleteAllForProject(UUID projectId);
}