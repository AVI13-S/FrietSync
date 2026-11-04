package com.frietsync.backend.dto.canvas;

import com.frietsync.backend.entity.issue.IssuePriority;
import lombok.Data;

@Data
public class ConvertToIssueRequest {

    private String title;
    private String description;
    private IssuePriority priority;
}