package com.frietsync.backend.dto.document;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class DocumentSummaryResponse {

    private UUID id;
    private UUID projectId;
    private String title;
    private String fileName;
    private String fileContentType;
    private Long fileSizeBytes;
    private String fileDownloadUrl;
    private UUID lastEditedBy;
    private String lastEditedByName;
    private Instant createdAt;
    private Instant updatedAt;
}