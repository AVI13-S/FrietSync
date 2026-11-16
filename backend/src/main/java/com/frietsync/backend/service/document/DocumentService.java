package com.frietsync.backend.service.document;

import com.frietsync.backend.dto.document.DocumentRequest;
import com.frietsync.backend.dto.document.DocumentResponse;
import com.frietsync.backend.dto.document.DocumentSummaryResponse;
import com.frietsync.backend.dto.document.UpdateDocumentRequest;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface DocumentService {

    record DocumentFile(String fileName, Resource resource) { }

    List<DocumentSummaryResponse> list(UUID projectId, UUID userId);

    DocumentResponse create(UUID projectId, DocumentRequest request, UUID userId);

    DocumentResponse uploadFile(UUID projectId, String title, MultipartFile file, UUID userId);

    DocumentResponse get(UUID documentId, UUID userId);

    DocumentFile downloadFile(UUID documentId, UUID userId);

    DocumentResponse update(UUID documentId, UpdateDocumentRequest request, UUID userId);

    void delete(UUID documentId, UUID userId);
}