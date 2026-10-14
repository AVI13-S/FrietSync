package com.frietsync.backend.repository.issue;

import com.frietsync.backend.entity.issue.IssueHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IssueHistoryRepository extends JpaRepository<IssueHistory, UUID> {

    List<IssueHistory> findByIssueIdOrderByCreatedAtDesc(UUID issueId);

    void deleteByIssueId(UUID issueId);
}
