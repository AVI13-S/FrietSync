package com.frietsync.backend.repository.issue;

import com.frietsync.backend.entity.issue.AssignmentRequestStatus;
import com.frietsync.backend.entity.issue.IssueAssignmentRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IssueAssignmentRequestRepository extends JpaRepository<IssueAssignmentRequest, UUID> {

    Optional<IssueAssignmentRequest> findByIssueIdAndUserId(UUID issueId, UUID userId);

    List<IssueAssignmentRequest> findByProjectIdAndStatusOrderByCreatedAtAsc(
            UUID projectId, AssignmentRequestStatus status);

    void deleteByIssueId(UUID issueId);
}
