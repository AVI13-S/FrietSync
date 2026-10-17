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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        projectAccess.require(project, userId, Role.PROJECT_MANAGER);

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
    @Transactional
    public DocumentResponse update(UUID documentId, UpdateDocumentRequest request, UUID userId) {
        ProjectDocument document = findDocument(documentId);
        Project project = projectAccess.project(document.getProjectId());
        projectAccess.require(project, userId, Role.PROJECT_MANAGER);

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

        return toResponse(document);
    }

    @Override
    @Transactional
    public void delete(UUID documentId, UUID userId) {
        ProjectDocument document = findDocument(documentId);
        Project project = projectAccess.project(document.getProjectId());
        projectAccess.require(project, userId, Role.PROJECT_MANAGER);
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
                .createdBy(document.getCreatedBy())
                .createdByName(names.get(document.getCreatedBy()))
                .lastEditedBy(document.getLastEditedBy())
                .lastEditedByName(names.get(document.getLastEditedBy()))
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    private Map<UUID, String> userNames(Set<UUID> ids) {
        Map<UUID, String> names = new HashMap<>();
        for (User user : userRepository.findAllById(ids)) {
            names.put(user.getId(), user.getName());
        }
        return names;
    }
}