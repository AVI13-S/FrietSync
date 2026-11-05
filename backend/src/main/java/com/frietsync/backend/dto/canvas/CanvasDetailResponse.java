package com.frietsync.backend.dto.canvas;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CanvasDetailResponse {

    private CanvasResponse canvas;
    private List<CanvasElementResponse> elements;
}