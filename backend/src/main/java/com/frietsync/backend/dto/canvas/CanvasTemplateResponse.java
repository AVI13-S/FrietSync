package com.frietsync.backend.dto.canvas;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CanvasTemplateResponse {

    private String key;
    private String name;
    private String description;
}