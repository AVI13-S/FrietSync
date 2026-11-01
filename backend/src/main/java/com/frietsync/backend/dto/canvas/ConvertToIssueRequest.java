package com.frietsync.backend.dto.canvas;

import com.frietsync.backend.entity.issue.IssuePriority;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConvertToIssueRequest {

    @Size(max = 200, message = "Title must be at most 200 characters")
    private String title;

    @Size(max = 1000, message = "Description must be at most 1000 characters")
    private String description;

    private IssuePriority priority;
}