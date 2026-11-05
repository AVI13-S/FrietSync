package com.frietsync.backend.dto.canvas;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CanvasEvent {

    private CanvasEventType type;
    private UUID canvasId;
    private UUID actorId;
    private List<CanvasElementResponse> elements;
    private List<UUID> elementIds;
    private String name;
    private Instant occurredAt;
}