package com.frietsync.backend.controller.issue;

import com.frietsync.backend.dto.issue.AttachmentResponse;
import com.frietsync.backend.service.issue.IssueAttachmentService;
import com.frietsync.backend.service.issue.IssueAttachmentService.AttachmentFile;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class IssueAttachmentController {

    private final IssueAttachmentService attachmentService;

    @PostMapping(value = "/issues/{issueId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentResponse> upload(@PathVariable UUID issueId,
                                                     @RequestParam("file") MultipartFile file,
                                                     @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(attachmentService.add(issueId, file, userId));
    }

    @GetMapping("/issues/{issueId}/attachments")
    public ResponseEntity<List<AttachmentResponse>> list(@PathVariable UUID issueId,
                                                         @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(attachmentService.list(issueId, userId));
    }

    @DeleteMapping("/attachments/{attachmentId}")
    public ResponseEntity<Void> delete(@PathVariable UUID attachmentId,
                                       @AuthenticationPrincipal UUID userId) {
        attachmentService.delete(attachmentId, userId);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID attachmentId,
                                             @AuthenticationPrincipal UUID userId) {
        AttachmentFile file = attachmentService.download(attachmentId, userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.fileName(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(file.resource());
    }
}