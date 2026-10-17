package com.frietsync.backend.dto.document;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class DocumentResponse {

    private UUID id;
    private UUID projectId;
    private String title;
    private String content;
    private UUID createdBy;
    private String createdByName;
    private UUID lastEditedBy;
    private String lastEditedByName;
    private Instant createdAt;
    private Instant updatedAt;
}