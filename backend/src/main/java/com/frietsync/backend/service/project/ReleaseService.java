package com.frietsync.backend.service.project;

import com.frietsync.backend.dto.project.AttachIssuesRequest;
import com.frietsync.backend.dto.project.ReleaseRequest;
import com.frietsync.backend.dto.project.ReleaseResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ReleaseService {
    ReleaseResponse create(UUID projectId, UUID userId, ReleaseRequest request);
    List<ReleaseResponse> list(UUID projectId, UUID userId);
    ReleaseResponse update(UUID releaseId, UUID userId, ReleaseRequest request);
    void delete(UUID releaseId, UUID userId);
    ReleaseResponse attachIssues(UUID releaseId, UUID userId, AttachIssuesRequest request);
    ReleaseResponse uploadFile(UUID releaseId, UUID userId, MultipartFile file);
}
