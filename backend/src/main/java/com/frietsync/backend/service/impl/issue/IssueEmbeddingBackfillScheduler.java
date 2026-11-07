package com.frietsync.backend.service.impl.issue;

import com.frietsync.backend.entity.issue.Issue;
import com.frietsync.backend.repository.issue.IssueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class IssueEmbeddingBackfillScheduler {

    private static final int BATCH_SIZE = 10;

    private final IssueRepository issueRepository;
    private final IssueEmbeddingService issueEmbeddingService;

    @Scheduled(
            fixedDelayString = "${app.ai.issue-embedding-backfill-delay-ms:60000}",
            initialDelayString = "${app.ai.issue-embedding-backfill-initial-delay-ms:60000}")
    public void backfillMissingEmbeddings() {
        List<Issue> issues = issueRepository.findIssuesWithoutEmbedding(BATCH_SIZE);
        for (Issue issue : issues) {
            issueEmbeddingService.generateAndSaveEmbeddingIfMissing(issue);
        }
    }
}
