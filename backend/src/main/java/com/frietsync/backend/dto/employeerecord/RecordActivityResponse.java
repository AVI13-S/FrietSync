package com.frietsync.backend.dto.employeerecord;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class RecordActivityResponse {

    private UUID userId;
    private String githubUrl;
    private boolean githubConnected;
    private int commits;
    private int pullRequests;
    private int mergedPullRequests;
    private int githubReviews;
    private long issueReviewsApproved;
    private long issueReviewsRejected;
    private Integer designSubmissions;
    private Integer designReviews;
}