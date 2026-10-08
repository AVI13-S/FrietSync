package com.frietsync.backend.dto.issue;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class AttachmentResponse {

    private UUID id;
    private UUID issueId;
    private String fileName;
    private String contentType;
    private long sizeBytes;
    private UUID uploadedBy;
    private Instant createdAt;
}
