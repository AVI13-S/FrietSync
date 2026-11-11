package com.frietsync.backend.controller.search;

import com.frietsync.backend.dto.issue.SemanticIssueResult;
import com.frietsync.backend.service.impl.search.SemanticSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SemanticSearchController {

    private final SemanticSearchService semanticSearchService;

    @GetMapping("/semantic")
    public List<SemanticIssueResult> search(@RequestParam("q") String query,
                                            @RequestParam(value = "projectId", required = false) UUID projectId,
                                            @RequestParam(value = "type", required = false) String type,
                                            @RequestParam(value = "status", required = false) String status,
                                            @AuthenticationPrincipal UUID userId) {
        return semanticSearchService.search(query, projectId, type, status, userId);
    }
}