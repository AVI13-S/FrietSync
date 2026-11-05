package com.frietsync.backend.dto.gantt;

import lombok.Data;

import java.util.UUID;

@Data
public class GanttLinkResponse {

    private UUID id;
    private UUID sourceIssueId;
    private UUID targetIssueId;
    private String type;
    private boolean critical;
    private boolean violated;
}