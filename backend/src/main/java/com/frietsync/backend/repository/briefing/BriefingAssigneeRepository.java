package com.frietsync.backend.repository.briefing;

import com.frietsync.backend.entity.issue.IssueAssignee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BriefingAssigneeRepository extends JpaRepository<IssueAssignee, UUID> {

    List<IssueAssignee> findByUserId(UUID userId);
}