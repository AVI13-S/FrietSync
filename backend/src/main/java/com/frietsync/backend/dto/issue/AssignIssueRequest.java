package com.frietsync.backend.dto.issue;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class AssignIssueRequest {

    @NotNull(message = "userIds is required; use an empty list to unassign everyone")
    @Size(max = 10, message = "An issue can have at most 10 assignees")
    private List<@NotNull UUID> userIds;
}
