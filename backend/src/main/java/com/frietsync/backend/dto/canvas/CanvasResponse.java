package com.frietsync.backend.dto.canvas;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class CanvasResponse {

    private UUID id;
    private UUID projectId;
    private String name;
    private UUID createdBy;
    private String createdByName;
    private long elementCount;
    private Instant lastActivityAt;
    private Instant createdAt;
    private Instant updatedAt;
}