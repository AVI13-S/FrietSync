package com.frietsync.backend.service.impl.document;

import com.frietsync.backend.dto.document.DocumentRequest;
import com.frietsync.backend.dto.document.DocumentResponse;
import com.frietsync.backend.dto.document.DocumentSummaryResponse;
import com.frietsync.backend.dto.document.UpdateDocumentRequest;
import com.frietsync.backend.entity.document.ProjectDocument;
import com.frietsync.backend.entity.project.Project;
import com.frietsync.backend.entity.user.Role;
import com.frietsync.backend.entity.user.User;
import com.frietsync.backend.exception.BadRequestException;
import com.frietsync.backend.exception.ResourceNotFoundException;
import com.frietsync.backend.repository.document.ProjectDocumentRepository;
import com.frietsync.backend.repository.user.UserRepository;
import com.frietsync.backend.service.document.DocumentService;
import com.frietsync.backend.service.impl.project.ProjectAccess;
import com.frietsync.backend.service.impl.issue.S3ObjectCleanupService;
import com.frietsync.backend.service.impl.storage.S3UploadValidator;
import com.frietsync.backend.service.security.VirusScanService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final ProjectAccess projectAccess;
    private final ProjectDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DocumentEmbeddingService documentEmbeddingService;
    private final DocumentTextExtractor documentTextExtractor;
    private final S3Client s3Client;
    private final S3ObjectCleanupService cleanupService;
    private final VirusScanService virusScanService;

    @Value("${aws.s3.bucket:}")
    private String bucketName;

    @Override
    @Transactional(readOnly = true)
    public List<DocumentSummaryResponse> list(UUID projectId, UUID userId) {
        Project project = projectAccess.project(projectId);
        projectAccess.requireMember(project, userId);

        List<ProjectDocument> documents = documentRepository.findByProjectIdOrderByUpdatedAtDesc(projectId);

        Set<UUID> userIds = new HashSet<>();
        for (ProjectDocument document : documents) {
            userIds.add(document.getLastEditedBy());
        }
        Map<UUID, String> names = userNames(userIds);

        List<DocumentSummaryResponse> result = new ArrayList<>();
        for (ProjectDocument document : documents) {
            result.add(DocumentSummaryResponse.builder()
                    .id(document.getId())
                    .projectId(document.getProjectId())
                    .title(document.getTitle())
                    .fileName(document.getFileName())
                    .fileContentType(document.getFileContentType())
                    .fileSizeBytes(document.getFileSizeBytes())
                    .fileDownloadUrl(fileDownloadUrl(document))
                    .lastEditedBy(document.getLastEditedBy())
                    .lastEditedByName(names.get(document.getLastEditedBy()))
                    .createdAt(document.getCreatedAt())
                    .updatedAt(document.getUpdatedAt())
                    .build());
        }
        return result;
    }

    @Override
    @Transactional
    public DocumentResponse create(UUID projectId, DocumentRequest request, UUID userId) {
        Project project = projectAccess.project(projectId);
        projectAccess.require(project, userId, Role.PROJECT_MANAGER, Role.TEAM_LEAD);

        ProjectDocument document = new ProjectDocument();
        document.setProjectId(projectId);
        document.setTitle(request.getTitle().trim());
        if (request.getContent() == null) {
            document.setContent("");
        } else {
            document.setContent(request.getContent());
        }
        document.setCreatedBy(userId);
        document.setLastEditedBy(userId);
        document = documentRepository.save(document);
        documentEmbeddingService.generateAndSaveChunks(document);

        return toResponse(document);
    }

    @Override
    @Transactional
    public DocumentResponse uploadFile(UUID projectId, String title, MultipartFile file, UUID userId) {
        Project project = projectAccess.project(projectId);
        projectAccess.require(project, userId, Role.PROJECT_MANAGER, Role.TEAM_LEAD);
        if (bucketName == null || bucketName.isBlank()) {
            throw new BadRequestException("S3 bucket is not configured");
        }

        String originalName = file == null ? null : file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new BadRequestException("Document file name is required");
        }
        String fileName = originalName.replace('\\', '/');
        fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
        fileName = fileName.replaceAll("[^A-Za-z0-9._-]", "_");
        if (fileName.length() > 255) {
            fileName = fileName.substring(fileName.length() - 255);
        }
        String contentType = S3UploadValidator.validateProjectDocument(file, fileName);
        String documentTitle = title == null || title.isBlank()
                ? titleFromFileName(fileName)
                : title.trim();
        if (documentTitle.isBlank() || documentTitle.length() > 200) {
            throw new BadRequestException("Document title must be between 1 and 200 characters");
        }

        virusScanService.scanFile(file);
        String extractedContent = documentTextExtractor.extract(file, fileName);

        String key = "project-documents/" + projectId + "/" + UUID.randomUUID() + "-" + fileName;
        try (java.io.InputStream input = file.getInputStream()) {
            s3Client.putObject(PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromInputStream(input, file.getSize()));
        } catch (IOException exception) {
            throw new BadRequestException("Could not read the uploaded document");
        }
        deleteS3ObjectOnRollback(key);

        ProjectDocument document = new ProjectDocument();
        document.setProjectId(projectId);
        document.setTitle(documentTitle);
        document.setContent(extractedContent);
        document.setFileKey(key);
        document.setFileName(fileName);
        document.setFileContentType(contentType);
        document.setFileSizeBytes(file.getSize());
        document.setCreatedBy(userId);
        document.setLastEditedBy(userId);
        document = documentRepository.save(document);
        documentEmbeddingService.generateAndSaveChunks(document);
        return toResponse(document);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponse get(UUID documentId, UUID userId) {
        ProjectDocument document = findDocument(documentId);
        Project project = projectAccess.project(document.getProjectId());
        projectAccess.requireMember(project, userId);
        return toResponse(document);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentFile downloadFile(UUID documentId, UUID userId) {
        ProjectDocument document = findDocument(documentId);
        Project project = projectAccess.project(document.getProjectId());
        projectAccess.requireMember(project, userId);
        if (document.getFileKey() == null || document.getFileName() == null) {
            throw new ResourceNotFoundException("Document file not found");
        }
        if (bucketName == null || bucketName.isBlank()) {
            throw new IllegalStateException("S3 bucket is not configured for project document downloads");
        }
        try {
            ResponseInputStream<GetObjectResponse> object = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(document.getFileKey())
                    .build());
            return new DocumentFile(document.getFileName(), new InputStreamResource(object));
        } catch (NoSuchKeyException exception) {
            throw new ResourceNotFoundException("Document file is no longer available");
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                throw new ResourceNotFoundException("Document file is no longer available");
            }
            throw exception;
        }
    }

    @Override
    @Transactional
    public DocumentResponse update(UUID documentId, UpdateDocumentRequest request, UUID userId) {
        ProjectDocument document = findDocument(documentId);
        Project project = projectAccess.project(document.getProjectId());
        projectAccess.require(project, userId, Role.PROJECT_MANAGER, Role.TEAM_LEAD);

        if (request.getTitle() == null && request.getContent() == null) {
            throw new BadRequestException("Nothing to update");
        }
        if (request.getTitle() != null) {
            if (request.getTitle().isBlank()) {
                throw new BadRequestException("Title must not be blank");
            }
            document.setTitle(request.getTitle().trim());
        }
        if (request.getContent() != null) {
            document.setContent(request.getContent());
        }
        document.setLastEditedBy(userId);
        document = documentRepository.save(document);
        documentEmbeddingService.generateAndSaveChunks(document);

        return toResponse(document);
    }

    @Override
    @Transactional
    public void delete(UUID documentId, UUID userId) {
        ProjectDocument document = findDocument(documentId);
        Project project = projectAccess.project(document.getProjectId());
        projectAccess.require(project, userId, Role.PROJECT_MANAGER);
        enqueueStoredFileDeletion(document.getFileKey());
        documentRepository.delete(document);
    }

    private ProjectDocument findDocument(UUID documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
    }

    private DocumentResponse toResponse(ProjectDocument document) {
        Set<UUID> userIds = new HashSet<>();
        userIds.add(document.getCreatedBy());
        userIds.add(document.getLastEditedBy());
        Map<UUID, String> names = userNames(userIds);

        return DocumentResponse.builder()
                .id(document.getId())
                .projectId(document.getProjectId())
                .title(document.getTitle())
                .content(document.getContent())
                .fileName(document.getFileName())
                .fileContentType(document.getFileContentType())
                .fileSizeBytes(document.getFileSizeBytes())
                .fileDownloadUrl(fileDownloadUrl(document))
                .createdBy(document.getCreatedBy())
                .createdByName(names.get(document.getCreatedBy()))
                .lastEditedBy(document.getLastEditedBy())
                .lastEditedByName(names.get(document.getLastEditedBy()))
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    private String fileDownloadUrl(ProjectDocument document) {
        return document.getFileKey() == null ? null : "/api/v1/documents/" + document.getId() + "/download";
    }

    private String titleFromFileName(String fileName) {
        int extensionIndex = fileName.lastIndexOf('.');
        String title = extensionIndex > 0 ? fileName.substring(0, extensionIndex) : fileName;
        return title.length() > 200 ? title.substring(0, 200) : title;
    }

    private void enqueueStoredFileDeletion(String key) {
        if (key != null && !key.isBlank()) {
            if (bucketName == null || bucketName.isBlank()) {
                throw new IllegalStateException("S3 bucket is not configured for project document cleanup");
            }
            cleanupService.enqueue(bucketName, key);
        }
    }

    private void deleteS3ObjectOnRollback(String key) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    cleanupService.enqueue(bucketName, key);
                }
            }
        });
    }

    private Map<UUID, String> userNames(Set<UUID> ids) {
        Map<UUID, String> names = new HashMap<>();
        for (User user : userRepository.findAllById(ids)) {
            names.put(user.getId(), user.getName());
        }
        return names;
    }
}