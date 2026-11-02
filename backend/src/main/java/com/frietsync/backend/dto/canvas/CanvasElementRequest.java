package com.frietsync.backend.dto.canvas;

import com.frietsync.backend.entity.canvas.CanvasElementType;
import com.frietsync.backend.entity.canvas.CanvasShape;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class CanvasElementRequest {

    @NotNull(message = "type is required")
    private CanvasElementType type;

    private CanvasShape shape;

    @NotNull(message = "x is required")
    @DecimalMin("-100000") @DecimalMax("100000")
    private Double x;

    @NotNull(message = "y is required")
    @DecimalMin("-100000") @DecimalMax("100000")
    private Double y;

    @DecimalMin(value = "20", message = "Width must be at least 20")
    @DecimalMax(value = "5000", message = "Width must be at most 5000")
    private Double width;

    @DecimalMin(value = "20", message = "Height must be at least 20")
    @DecimalMax(value = "5000", message = "Height must be at most 5000")
    private Double height;

    @DecimalMin("-100000") @DecimalMax("100000")
    private Double endX;

    @DecimalMin("-100000") @DecimalMax("100000")
    private Double endY;

    @DecimalMin("-360") @DecimalMax("360")
    private Double rotation;

    private Integer zIndex;

    @Size(max = 2000, message = "Text must be at most 2000 characters")
    private String text;

    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Color must be a hex value.")
    private String color;

    private UUID fromElementId;
    private UUID toElementId;
}