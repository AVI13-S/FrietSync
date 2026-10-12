package com.frietsync.backend.dto.issue;

import com.frietsync.backend.entity.issue.IssueLinkType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class IssueLinkRequest {

    @NotNull(message = "targetIssueId is required")
    private UUID targetIssueId;

    @NotNull(message = "type is required")
    private IssueLinkType type;
}