package com.frietsync.backend.dto.rag;

import lombok.Data;

import java.util.UUID;

@Data
public class RagSourceResponse {

    private UUID documentId;
    private String title;
}