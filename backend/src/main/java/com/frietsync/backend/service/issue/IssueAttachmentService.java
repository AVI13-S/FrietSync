package com.frietsync.backend.service.issue;

import com.frietsync.backend.dto.issue.AttachmentResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface IssueAttachmentService {

    record AttachmentFile(String fileName, String contentType, Resource resource) { }

    AttachmentResponse add(UUID issueId, MultipartFile file, UUID userId);

    List<AttachmentResponse> list(UUID issueId, UUID userId);

    void delete(UUID attachmentId, UUID userId);

    AttachmentFile download(UUID attachmentId, UUID userId);

    void deleteAllForIssue(UUID issueId);
}
