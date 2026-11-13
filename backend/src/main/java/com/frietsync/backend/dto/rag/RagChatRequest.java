package com.frietsync.backend.dto.rag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class RagChatRequest {

    private UUID requestId;

    private UUID projectId; // Optional: target a specific project, or null for all accessible projects

    @NotBlank
    @Size(max = 1000)
    private String question;
}
