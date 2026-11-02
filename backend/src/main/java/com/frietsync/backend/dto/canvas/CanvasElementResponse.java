package com.frietsync.backend.dto.canvas;

import com.frietsync.backend.dto.issue.IssueSummaryResponse;
import com.frietsync.backend.entity.canvas.CanvasElementType;
import com.frietsync.backend.entity.canvas.CanvasShape;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CanvasElementResponse {

    private UUID id;
    private UUID canvasId;
    private CanvasElementType type;
    private CanvasShape shape;
    private double x;
    private double y;
    private Double width;
    private Double height;
    private Double endX;
    private Double endY;
    private double rotation;
    private int zIndex;
    private String text;
    private String color;
    private UUID fromElementId;
    private UUID toElementId;
    private IssueSummaryResponse linkedIssue;
    private UUID createdBy;
    private UUID updatedBy;
    private long revision;
}