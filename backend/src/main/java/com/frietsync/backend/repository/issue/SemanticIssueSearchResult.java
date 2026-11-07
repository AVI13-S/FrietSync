package com.frietsync.backend.repository.issue;

import java.util.UUID;

public interface SemanticIssueSearchResult {

    UUID getIssueId();

    UUID getProjectId();

    String getProjectName();

    String getTitle();

    String getDescription();

    String getStatus();

    String getPriority();

    double getSimilarity();
}
