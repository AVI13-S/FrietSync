package com.frietsync.backend.dto.issue;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AssignmentDecisionRequest {

    @NotNull(message = "requestId is required")
    private UUID requestId;

    @NotNull(message = "approve is required")
    private Boolean approve;
}
