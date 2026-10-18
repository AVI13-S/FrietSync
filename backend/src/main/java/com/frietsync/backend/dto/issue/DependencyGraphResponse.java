package com.frietsync.backend.dto.issue;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DependencyGraphResponse {

    private List<IssueSummaryResponse> nodes;
    private List<IssueLinkResponse> edges;
}