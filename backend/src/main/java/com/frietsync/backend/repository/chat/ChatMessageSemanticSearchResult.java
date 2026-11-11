package com.frietsync.backend.repository.chat;

import java.util.UUID;

public interface ChatMessageSemanticSearchResult {

    UUID getMessageId();

    UUID getProjectId();

    String getProjectName();

    String getTitle();

    String getDescription();

    double getSimilarity();
}