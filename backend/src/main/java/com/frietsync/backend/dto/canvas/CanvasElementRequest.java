package com.frietsync.backend.dto.canvas;

import com.frietsync.backend.entity.canvas.CanvasElementType;
import lombok.Data;

import java.util.UUID;

@Data
public class CanvasElementRequest {

    private CanvasElementType type;
    private String shape;
    private Double x;
    private Double y;
    private Double width;
    private Double height;

    private Double endX;
    private Double endY;
    private UUID fromElementId;
    private UUID toElementId;

    private Double rotation;
    private Integer zIndex;
    private String text;
    private String color;
}