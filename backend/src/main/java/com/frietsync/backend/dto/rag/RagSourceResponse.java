package com.frietsync.backend.dto.rag;

import lombok.Data;

import java.util.UUID;

@Data
public class RagSourceResponse {

    private String type;
    private UUID id;
    private UUID documentId;
    private UUID projectId;
    private String projectName;
    private String title;
}
