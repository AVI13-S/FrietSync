package com.frietsync.backend.service.impl.issue;

import com.frietsync.backend.dto.issue.AttachmentResponse;
import com.frietsync.backend.entity.issue.Issue;
import com.frietsync.backend.entity.issue.IssueAttachment;
import com.frietsync.backend.entity.issue.IssueHistoryAction;
import com.frietsync.backend.entity.user.Role;
import com.frietsync.backend.exception.BadRequestException;
import com.frietsync.backend.exception.ForbiddenException;
import com.frietsync.backend.exception.ResourceNotFoundException;
import com.frietsync.backend.repository.issue.IssueAttachmentRepository;
import com.frietsync.backend.service.impl.project.ProjectAccess;
import com.frietsync.backend.service.issue.IssueAttachmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class IssueAttachmentServiceImpl implements IssueAttachmentService {

    private final IssueAttachmentRepository attachmentRepository;
    private final ProjectAccess access;
    private final IssueHistoryRecorder history;

    @Value("${app.storage.path:./uploads}")
    private String storagePath;

    @Override
    public AttachmentResponse add(UUID issueId, MultipartFile file, UUID userId) {
        Issue issue = access.issue(issueId);
        access.requireMember(access.project(issue.getProjectId()), userId);
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        String key = issue.getProjectId() + "/" + UUID.randomUUID();
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target);
            }
        } catch (IOException e) {
            log.error("Could not store attachment", e);
            throw new IllegalStateException("Could not store the file");
        }

        String contentType = file.getContentType();
        if (contentType == null || contentType.length() > 100) {
            contentType = "application/octet-stream";
        }

        IssueAttachment attachment = new IssueAttachment();
        attachment.setIssueId(issueId);
        attachment.setUploadedBy(userId);
        attachment.setFileName(sanitize(file.getOriginalFilename()));
        attachment.setContentType(contentType);
        attachment.setSizeBytes(file.getSize());
        attachment.setStorageKey(key);
        attachmentRepository.save(attachment);

        history.record(issueId, userId, IssueHistoryAction.ATTACHMENT_ADDED,
                "attachment", null, attachment.getFileName());
        return toResponse(attachment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentResponse> list(UUID issueId, UUID userId) {
        Issue issue = access.issue(issueId);
        access.requireMember(access.project(issue.getProjectId()), userId);
        return attachmentRepository.findByIssueIdOrderByCreatedAtDesc(issueId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public void delete(UUID attachmentId, UUID userId) {
        IssueAttachment attachment = find(attachmentId);
        Issue issue = access.issue(attachment.getIssueId());
        Role role = access.requireMember(access.project(issue.getProjectId()), userId);

        boolean allowed = userId.equals(attachment.getUploadedBy())
                || role == Role.ADMIN
                || role == Role.PROJECT_MANAGER;
        if (!allowed) {
            throw new ForbiddenException("Only the uploader or project manager can remove this attachment");
        }

        deleteFile(attachment.getStorageKey());
        attachmentRepository.delete(attachment);
        history.record(issue.getId(), userId, IssueHistoryAction.ATTACHMENT_REMOVED,
                "attachment", attachment.getFileName(), null);
    }

    @Override
    @Transactional(readOnly = true)
    public AttachmentFile download(UUID attachmentId, UUID userId) {
        IssueAttachment attachment = find(attachmentId);
        Issue issue = access.issue(attachment.getIssueId());
        access.requireMember(access.project(issue.getProjectId()), userId);

        Path path = resolve(attachment.getStorageKey());
        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("File is no longer available");
        }
        return new AttachmentFile(attachment.getFileName(), attachment.getContentType(),
                new FileSystemResource(path));
    }

    @Override
    public void deleteAllForIssue(UUID issueId) {
        List<IssueAttachment> all = attachmentRepository.findByIssueIdOrderByCreatedAtDesc(issueId);
        all.forEach(a -> deleteFile(a.getStorageKey()));
        attachmentRepository.deleteAll(all);
    }

    private IssueAttachment find(UUID attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));
    }

    private Path resolve(String key) {
        Path base = Path.of(storagePath).toAbsolutePath().normalize();
        Path path = base.resolve(key).normalize();
        if (!path.startsWith(base)) {
            throw new IllegalStateException("Invalid storage path");
        }
        return path;
    }

    private void deleteFile(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            log.warn("Could not delete attachment file {}", key, e);
        }
    }

    private String sanitize(String original) {
        String name = original == null ? "" : original.replaceAll("[\\\\/\\r\\n\"]", "_").trim();
        if (name.isEmpty()) {
            return "file";
        }
        return name.length() > 255 ? name.substring(0, 255) : name;
    }

    private AttachmentResponse toResponse(IssueAttachment a) {
        return AttachmentResponse.builder()
                .id(a.getId())
                .issueId(a.getIssueId())
                .fileName(a.getFileName())
                .contentType(a.getContentType())
                .sizeBytes(a.getSizeBytes())
                .uploadedBy(a.getUploadedBy())
                .createdAt(a.getCreatedAt())
                .build();
    }
}