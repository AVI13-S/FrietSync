package com.frietsync.backend.service.document;

import com.frietsync.backend.dto.document.DocumentRequest;
import com.frietsync.backend.dto.document.DocumentResponse;
import com.frietsync.backend.dto.document.DocumentSummaryResponse;
import com.frietsync.backend.dto.document.UpdateDocumentRequest;

import java.util.List;
import java.util.UUID;

public interface DocumentService {

    List<DocumentSummaryResponse> list(UUID projectId, UUID userId);

    DocumentResponse create(UUID projectId, DocumentRequest request, UUID userId);

    DocumentResponse get(UUID documentId, UUID userId);

    DocumentResponse update(UUID documentId, UpdateDocumentRequest request, UUID userId);

    void delete(UUID documentId, UUID userId);
}