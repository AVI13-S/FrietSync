package com.frietsync.backend.dto.canvas;

import com.frietsync.backend.dto.issue.IssueResponse;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConvertToIssueResponse {

    private CanvasElementResponse element;
    private IssueResponse issue;
}