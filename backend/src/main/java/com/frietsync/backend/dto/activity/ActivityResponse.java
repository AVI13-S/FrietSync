package com.frietsync.backend.dto.activity;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ActivityResponse {

    private UUID id;
    private String resourceType;
    private UUID resourceId;
    private String resourceName;
    private String message;
    private UUID issueId;
    private int issueNumber;
    private String issueTitle;
    private UUID actorId;
    private String actorName;
    private String action;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private String comment;
    private Instant createdAt;
}
