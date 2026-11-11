package com.frietsync.backend.repository.document;

import java.util.UUID;

public interface DocumentSemanticSearchResult {

    UUID getDocumentId();

    UUID getProjectId();

    String getProjectName();

    String getTitle();

    String getDescription();

    double getSimilarity();
}