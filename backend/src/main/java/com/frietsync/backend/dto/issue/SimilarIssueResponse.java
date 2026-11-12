package com.frietsync.backend.dto.issue;

import java.util.UUID;

public record SimilarIssueResponse(
        UUID issueId,
        String title,
        String status,
        String priority,
        double similarity
) {
}