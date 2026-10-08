package com.frietsync.backend.dto.issue;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class AssignIssueRequest {

    @NotEmpty(message = "At least one user is required")
    private List<UUID> userIds;
}
