package com.frietsync.backend.repository.issue;

import com.frietsync.backend.entity.issue.IssueAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IssueAttachmentRepository extends JpaRepository<IssueAttachment, UUID> {

    List<IssueAttachment> findByIssueIdOrderByCreatedAtDesc(UUID issueId);
}
