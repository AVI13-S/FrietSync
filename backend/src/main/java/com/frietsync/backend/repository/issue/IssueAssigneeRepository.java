package com.frietsync.backend.repository.issue;

import com.frietsync.backend.entity.issue.IssueAssignee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface IssueAssigneeRepository extends JpaRepository<IssueAssignee, UUID> {

    List<IssueAssignee> findByIssueId(UUID issueId);

    List<IssueAssignee> findByIssueIdIn(Collection<UUID> issueIds);

    boolean existsByIssueIdAndUserId(UUID issueId, UUID userId);

    void deleteByIssueId(UUID issueId);
}
