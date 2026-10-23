package com.frietsync.backend.dto.issue;

import java.util.UUID;

public record IssueAssigneeResponse(UUID userId, String name) {
}
