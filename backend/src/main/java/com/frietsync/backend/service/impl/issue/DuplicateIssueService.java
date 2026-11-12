package com.frietsync.backend.service.impl.issue;

import com.frietsync.backend.dto.issue.SimilarIssueResponse;
import com.frietsync.backend.entity.project.Project;
import com.frietsync.backend.exception.BadRequestException;
import com.frietsync.backend.repository.issue.IssueRepository;
import com.frietsync.backend.repository.issue.SemanticIssueSearchResult;
import com.frietsync.backend.service.impl.project.ProjectAccess;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DuplicateIssueService {

    private static final Logger LOG = LoggerFactory.getLogger(DuplicateIssueService.class);
    private static final int MAX_RESULTS = 5;
    private static final int MAX_DESCRIPTION_LENGTH = 2000;
    private static final int MAX_CHECKS_PER_MINUTE = 30;
    private static final long ONE_MINUTE_MS = 60000;

    private final ProjectAccess access;
    private final EmbeddingModel embeddingModel;
    private final IssueRepository issueRepository;

    @Value("${app.ai.duplicate-min-similarity:0.7}")
    private double minSimilarity;

    private final Map<UUID, List<Long>> recentChecks = new HashMap<>();

    public List<SimilarIssueResponse> findSimilarIssues(UUID projectId, String title, String description,
                                                        UUID userId) {
        Project project = access.project(projectId);
        access.requireMember(project, userId);

        if (title == null || title.isBlank()) {
            throw new BadRequestException("Title is required");
        }
        checkRateLimit(userId);

        StringBuilder text = new StringBuilder("Title: ").append(title.trim());
        if (description != null && !description.isBlank()) {
            String shortDescription = description.trim();
            if (shortDescription.length() > MAX_DESCRIPTION_LENGTH) {
                shortDescription = shortDescription.substring(0, MAX_DESCRIPTION_LENGTH);
            }
            text.append("\nDescription: ").append(shortDescription);
        }

        String vector;
        try {
            vector = formatVector(embeddingModel.embed(text.toString()));
        } catch (RuntimeException ex) {
            LOG.warn("Duplicate check embedding failed for project {}: {}", projectId, ex.getMessage());
            return new ArrayList<>();
        }

        List<SimilarIssueResponse> similarIssues = new ArrayList<>();
        for (SemanticIssueSearchResult row : issueRepository.searchSemanticallyInProject(projectId, vector, MAX_RESULTS)) {
            if (row.getSimilarity() >= minSimilarity) {
                similarIssues.add(new SimilarIssueResponse(
                        row.getIssueId(),
                        row.getTitle(),
                        row.getStatus(),
                        row.getPriority(),
                        row.getSimilarity()));
            }
        }
        return similarIssues;
    }

    private synchronized void checkRateLimit(UUID userId) {
        long now = System.currentTimeMillis();
        List<Long> times = new ArrayList<>();
        List<Long> oldTimes = recentChecks.get(userId);
        if (oldTimes != null) {
            for (Long time : oldTimes) {
                if (now - time < ONE_MINUTE_MS) {
                    times.add(time);
                }
            }
        }
        if (times.size() >= MAX_CHECKS_PER_MINUTE) {
            recentChecks.put(userId, times);
            throw new BadRequestException("Too many duplicate checks. Please wait a minute and try again.");
        }
        times.add(now);
        recentChecks.put(userId, times);
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