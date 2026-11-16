package com.frietsync.backend.controller.document;

import com.frietsync.backend.dto.document.DocumentRequest;
import com.frietsync.backend.dto.document.DocumentResponse;
import com.frietsync.backend.dto.document.DocumentSummaryResponse;
import com.frietsync.backend.dto.document.UpdateDocumentRequest;
import com.frietsync.backend.service.document.DocumentService;
import com.frietsync.backend.service.document.DocumentService.DocumentFile;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
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

    @PostMapping(value = "/projects/{projectId}/documents/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> uploadFile(@PathVariable UUID projectId,
                                                        @RequestParam("file") MultipartFile file,
                                                        @RequestParam(required = false) String title,
                                                        @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.uploadFile(projectId, title, file, userId));
    }

    @GetMapping("/documents/{documentId}")
    public ResponseEntity<DocumentResponse> get(@PathVariable UUID documentId,
                                                @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(documentService.get(documentId, userId));
    }

    @GetMapping("/documents/{documentId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable UUID documentId,
                                                 @AuthenticationPrincipal UUID userId) {
        DocumentFile file = documentService.downloadFile(documentId, userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.fileName(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(file.resource());
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