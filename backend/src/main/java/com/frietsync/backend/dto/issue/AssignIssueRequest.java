package com.frietsync.backend.dto.issue;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class AssignIssueRequest {

    @NotNull(message = "userIds is required; use an empty list to unassign everyone")
    private List<UUID> userIds;
}
