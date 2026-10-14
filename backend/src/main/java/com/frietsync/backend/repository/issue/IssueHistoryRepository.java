package com.frietsync.backend.repository.issue;

import com.frietsync.backend.entity.issue.IssueHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface IssueHistoryRepository extends JpaRepository<IssueHistory, UUID> {

    Page<IssueHistory> findByIssueIdOrderByCreatedAtDescIdDesc(UUID issueId, Pageable pageable);

    void deleteByIssueId(UUID issueId);
}
