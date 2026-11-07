package com.frietsync.backend.service.impl.search;

import com.frietsync.backend.dto.issue.SemanticIssueResult;
import com.frietsync.backend.exception.AiServiceException;
import com.frietsync.backend.exception.BadRequestException;
import com.frietsync.backend.repository.issue.IssueRepository;
import com.frietsync.backend.repository.issue.SemanticIssueSearchResult;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SemanticSearchService {

    private static final Logger LOG = LoggerFactory.getLogger(SemanticSearchService.class);
    private static final int MAX_QUERY_LENGTH = 1000;
    private static final int MAX_RESULTS = 20;

    private final EmbeddingModel embeddingModel;
    private final IssueRepository issueRepository;

    @Transactional(readOnly = true)
    public List<SemanticIssueResult> search(String query, UUID userId) {
        if (query == null || query.isBlank()) {
            throw new BadRequestException("Search query is required");
        }
        String normalizedQuery = query.trim();
        if (normalizedQuery.length() > MAX_QUERY_LENGTH) {
            throw new BadRequestException("Search query is too long (max " + MAX_QUERY_LENGTH + " characters)");
        }

        float[] embedding;
        try {
            embedding = embeddingModel.embed(normalizedQuery);
        } catch (RuntimeException ex) {
            LOG.error("Embedding semantic search query failed for user {}", userId, ex);
            throw new AiServiceException("The AI service is not available right now. Please try again later.");
        }

        return issueRepository.searchSemanticallyForUser(userId, formatVector(embedding), MAX_RESULTS)
                .stream()
                .map(this::toResult)
                .toList();
    }

    private SemanticIssueResult toResult(SemanticIssueSearchResult result) {
        return new SemanticIssueResult(
                result.getIssueId(),
                result.getProjectId(),
                result.getProjectName(),
                result.getTitle(),
                result.getDescription(),
                result.getStatus(),
                result.getPriority(),
                result.getSimilarity()
        );
    }

    private String formatVector(float[] vector) {
        StringBuilder result = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                result.append(",");
            }
            result.append(vector[i]);
        }
        return result.append("]").toString();
    }
}
