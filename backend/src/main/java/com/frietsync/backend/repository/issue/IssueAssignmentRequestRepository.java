package com.frietsync.backend.repository.issue;

import com.frietsync.backend.entity.issue.AssignmentRequestStatus;
import com.frietsync.backend.entity.issue.IssueAssignmentRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IssueAssignmentRequestRepository extends JpaRepository<IssueAssignmentRequest, UUID> {

    Optional<IssueAssignmentRequest> findByIssueIdAndUserId(UUID issueId, UUID userId);

    List<IssueAssignmentRequest> findByProjectIdAndUserIdAndStatus(
            UUID projectId, UUID userId, AssignmentRequestStatus status);

    List<IssueAssignmentRequest> findByProjectIdAndStatusOrderByCreatedAtAsc(
            UUID projectId, AssignmentRequestStatus status);

    void deleteByIssueId(UUID issueId);

    @Modifying
    @Query("delete from IssueAssignmentRequest request where request.status = :status "
            + "and request.createdAt < :cutoff")
    int deleteExpiredPendingRequests(@Param("status") AssignmentRequestStatus status,
                                     @Param("cutoff") java.time.Instant cutoff);
}
