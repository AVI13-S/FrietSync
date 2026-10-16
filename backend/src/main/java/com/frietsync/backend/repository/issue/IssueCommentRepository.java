package com.frietsync.backend.repository.issue;

import com.frietsync.backend.entity.issue.IssueComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IssueCommentRepository extends JpaRepository<IssueComment, UUID> {
    List<IssueComment> findByIssueIdOrderByCreatedAtAsc(UUID issueId);
}
