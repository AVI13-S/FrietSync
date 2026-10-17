package com.frietsync.backend.controller.document;

import com.frietsync.backend.dto.document.DocumentRequest;
import com.frietsync.backend.dto.document.DocumentResponse;
import com.frietsync.backend.dto.document.DocumentSummaryResponse;
import com.frietsync.backend.dto.document.UpdateDocumentRequest;
import com.frietsync.backend.service.document.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping("/projects/{projectId}/documents")
    public ResponseEntity<List<DocumentSummaryResponse>> list(@PathVariable UUID projectId,
                                                              @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(documentService.list(projectId, userId));
    }

    @PostMapping("/projects/{projectId}/documents")
    public ResponseEntity<DocumentResponse> create(@PathVariable UUID projectId,
                                                   @Valid @RequestBody DocumentRequest request,
                                                   @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.create(projectId, request, userId));
    }

    @GetMapping("/documents/{documentId}")
    public ResponseEntity<DocumentResponse> get(@PathVariable UUID documentId,
                                                @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(documentService.get(documentId, userId));
    }

    @PatchMapping("/documents/{documentId}")
    public ResponseEntity<DocumentResponse> update(@PathVariable UUID documentId,
                                                   @Valid @RequestBody UpdateDocumentRequest request,
                                                   @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(documentService.update(documentId, request, userId));
    }

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<Void> delete(@PathVariable UUID documentId,
                                       @AuthenticationPrincipal UUID userId) {
        documentService.delete(documentId, userId);
        return ResponseEntity.noContent().build();
    }
}