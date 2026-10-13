package com.frietsync.backend.repository.issue;

import com.frietsync.backend.entity.issue.IssueHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;
import java.time.Instant;
import java.util.Collection;

public interface IssueHistoryRepository extends JpaRepository<IssueHistory, UUID> {

    List<IssueHistory> findByIssueIdOrderByCreatedAtDesc(UUID issueId);

    void deleteByIssueId(UUID issueId);

    List<IssueHistory> findByIssueIdInOrderByCreatedAtDesc(Collection<UUID> issueIds, Pageable pageable);

    List<IssueHistory> findByIssueIdInAndCreatedAtBetweenOrderByCreatedAtDesc(
            Collection<UUID> issueIds, Instant from, Instant to);
}
