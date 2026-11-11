package com.frietsync.backend.service.impl.search;

import com.frietsync.backend.dto.issue.SemanticIssueResult;
import com.frietsync.backend.exception.AiServiceException;
import com.frietsync.backend.exception.BadRequestException;
import com.frietsync.backend.repository.chat.ChatMessageRepository;
import com.frietsync.backend.repository.chat.ChatMessageSemanticSearchResult;
import com.frietsync.backend.repository.document.DocumentSemanticSearchResult;
import com.frietsync.backend.repository.document.ProjectDocumentChunkRepository;
import com.frietsync.backend.repository.issue.IssueRepository;
import com.frietsync.backend.repository.issue.SemanticIssueSearchResult;
import com.frietsync.backend.repository.sprint.SprintRepository;
import com.frietsync.backend.repository.sprint.SprintSemanticSearchResult;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SemanticSearchService {

    private static final Logger LOG = LoggerFactory.getLogger(SemanticSearchService.class);
    private static final int MAX_QUERY_LENGTH = 1000;
    private static final int MAX_RESULTS = 20;

    private static final String TYPE_ISSUE = "ISSUE";
    private static final String TYPE_SPRINT = "SPRINT";
    private static final String TYPE_DOCUMENT = "DOCUMENT";
    private static final String TYPE_CHAT_MESSAGE = "CHAT_MESSAGE";

    private final EmbeddingModel embeddingModel;
    private final IssueRepository issueRepository;
    private final SprintRepository sprintRepository;
    private final ProjectDocumentChunkRepository documentChunkRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Value("${app.ai.search-min-similarity:0.3}")
    private double minSimilarity;

    @Transactional(readOnly = true)
    public List<SemanticIssueResult> search(String query, UUID userId) {
        return search(query, null, null, null, userId);
    }

    @Transactional(readOnly = true)
    public List<SemanticIssueResult> search(String query, UUID projectId, String type, String status,
                                            UUID userId) {
        if (query == null || query.isBlank()) {
            throw new BadRequestException("Search query is required");
        }
        String normalizedQuery = query.trim();
        if (normalizedQuery.length() > MAX_QUERY_LENGTH) {
            throw new BadRequestException("Search query is too long (max " + MAX_QUERY_LENGTH + " characters)");
        }

        String typeFilter = normalize(type);
        if (typeFilter != null && !typeFilter.equals(TYPE_ISSUE) && !typeFilter.equals(TYPE_SPRINT)
                && !typeFilter.equals(TYPE_DOCUMENT) && !typeFilter.equals(TYPE_CHAT_MESSAGE)) {
            throw new BadRequestException("Type must be ISSUE, SPRINT, DOCUMENT or CHAT_MESSAGE");
        }
        String statusFilter = normalize(status);
        String projectFilter = null;
        if (projectId != null) {
            projectFilter = projectId.toString();
        }

        float[] embedding;
        try {
            embedding = embeddingModel.embed(normalizedQuery);
        } catch (RuntimeException ex) {
            LOG.error("Embedding semantic search query failed for user {}", userId, ex);
            throw new AiServiceException("The AI service is not available right now. Please try again later.");
        }

        String vector = formatVector(embedding);
        List<SemanticIssueResult> results = new ArrayList<>();

        if (typeFilter == null || typeFilter.equals(TYPE_ISSUE)) {
            List<SemanticIssueResult> issueResults = issueRepository
                    .searchSemanticallyForUser(userId, vector, projectFilter, statusFilter, MAX_RESULTS)
                    .stream()
                    .filter(row -> row.getSimilarity() >= minSimilarity)
                    .map(this::toResult)
                    .collect(Collectors.toList());
            results.addAll(issueResults);
        }

        if (typeFilter == null || typeFilter.equals(TYPE_SPRINT)) {
            List<SemanticIssueResult> sprintResults = sprintRepository
                    .searchSemanticallyForUser(userId, vector, projectFilter, statusFilter, MAX_RESULTS)
                    .stream()
                    .filter(row -> row.getSimilarity() >= minSimilarity)
                    .map(this::toSprintResult)
                    .collect(Collectors.toList());
            results.addAll(sprintResults);
        }

        if (statusFilter == null && (typeFilter == null || typeFilter.equals(TYPE_DOCUMENT))) {
            List<SemanticIssueResult> documentResults = documentChunkRepository
                    .searchDocumentsForUser(userId, vector, projectFilter, MAX_RESULTS)
                    .stream()
                    .filter(row -> row.getSimilarity() >= minSimilarity)
                    .map(this::toDocumentResult)
                    .collect(Collectors.toList());
            results.addAll(documentResults);
        }

        if (statusFilter == null && (typeFilter == null || typeFilter.equals(TYPE_CHAT_MESSAGE))) {
            List<SemanticIssueResult> chatResults = chatMessageRepository
                    .searchSemanticallyForUser(userId, vector, projectFilter, MAX_RESULTS)
                    .stream()
                    .filter(row -> row.getSimilarity() >= minSimilarity)
                    .map(this::toChatMessageResult)
                    .collect(Collectors.toList());
            results.addAll(chatResults);
        }

        return results.stream()
                .sorted(Comparator.comparingDouble(SemanticIssueResult::similarity).reversed())
                .limit(MAX_RESULTS)
                .toList();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private SemanticIssueResult toResult(SemanticIssueSearchResult result) {
        return new SemanticIssueResult(
                TYPE_ISSUE,
                result.getIssueId(),
                null,
                result.getProjectId(),
                result.getProjectName(),
                result.getTitle(),
                result.getDescription(),
                result.getStatus(),
                result.getPriority(),
                result.getSimilarity(),
                null,
                null
        );
    }

    private SemanticIssueResult toSprintResult(SprintSemanticSearchResult result) {
        return new SemanticIssueResult(
                TYPE_SPRINT,
                null,
                result.getSprintId(),
                result.getProjectId(),
                result.getProjectName(),
                result.getTitle(),
                result.getDescription(),
                result.getStatus(),
                result.getPriority(),
                result.getSimilarity(),
                null,
                null
        );
    }

    private SemanticIssueResult toDocumentResult(DocumentSemanticSearchResult result) {
        return new SemanticIssueResult(
                TYPE_DOCUMENT,
                null,
                null,
                result.getProjectId(),
                result.getProjectName(),
                result.getTitle(),
                result.getDescription(),
                null,
                null,
                result.getSimilarity(),
                result.getDocumentId(),
                null
        );
    }

    private SemanticIssueResult toChatMessageResult(ChatMessageSemanticSearchResult result) {
        return new SemanticIssueResult(
                TYPE_CHAT_MESSAGE,
                null,
                null,
                result.getProjectId(),
                result.getProjectName(),
                result.getTitle(),
                result.getDescription(),
                null,
                null,
                result.getSimilarity(),
                null,
                result.getMessageId()
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