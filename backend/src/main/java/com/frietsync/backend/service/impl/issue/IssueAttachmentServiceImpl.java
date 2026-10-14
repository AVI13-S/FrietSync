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
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

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
    private final S3Client s3Client;

    @Value("${app.storage.path:./uploads}")
    private String storagePath;

    @Value("${aws.s3.bucket:}")
    private String bucketName;

    @Override
    public AttachmentResponse add(UUID issueId, MultipartFile file, UUID userId) {
        Issue issue = access.issue(issueId);
        access.requireMember(access.project(issue.getProjectId()), userId);
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        if (bucketName == null || bucketName.isBlank()) {
            throw new BadRequestException("S3 bucket is not configured");
        }

        String key = "issues/" + issue.getProjectId() + "/" + issueId + "/" + UUID.randomUUID();
        String contentType = file.getContentType();
        if (contentType == null || contentType.length() > 100) {
            contentType = "application/octet-stream";
        }
        try {
            try (InputStream input = file.getInputStream()) {
                s3Client.putObject(PutObjectRequest.builder()
                                .bucket(bucketName)
                                .key(key)
                                .contentType(contentType)
                                .build(),
                        RequestBody.fromInputStream(input, file.getSize()));
            }
        } catch (IOException e) {
            log.error("Could not read issue attachment for S3 upload", e);
            throw new IllegalStateException("Could not read the uploaded file");
        }
        deleteS3ObjectOnRollback(key);

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

        if (isS3Key(attachment.getStorageKey())) {
            try {
                ResponseInputStream<GetObjectResponse> object = s3Client.getObject(GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(attachment.getStorageKey())
                        .build());
                return new AttachmentFile(attachment.getFileName(), attachment.getContentType(),
                        new InputStreamResource(object));
            } catch (NoSuchKeyException exception) {
                throw new ResourceNotFoundException("File is no longer available");
            } catch (S3Exception exception) {
                if (exception.statusCode() == 404) {
                    throw new ResourceNotFoundException("File is no longer available");
                }
                throw exception;
            }
        }

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
        if (isS3Key(key)) {
            runAfterCommit(() -> deleteS3Object(key));
            return;
        }
        runAfterCommit(() -> deleteLocalFile(key));
    }

    private void deleteLocalFile(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            log.warn("Could not delete attachment file {}", key, e);
        }
    }

    private boolean isS3Key(String key) {
        return key != null && key.startsWith("issues/");
    }

    private void deleteS3ObjectOnRollback(String key) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    deleteS3Object(key);
                }
            }
        });
    }

    private void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private void deleteS3Object(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(key).build());
        } catch (RuntimeException exception) {
            log.error("Could not delete issue attachment {}",exception);
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