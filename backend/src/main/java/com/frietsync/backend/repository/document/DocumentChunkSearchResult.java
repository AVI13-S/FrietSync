package com.frietsync.backend.repository.document;

import java.util.UUID;

public interface DocumentChunkSearchResult {

    UUID getDocumentId();

    String getTitle();

    String getContent();
}
