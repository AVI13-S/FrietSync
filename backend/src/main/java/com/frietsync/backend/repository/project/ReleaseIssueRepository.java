package com.frietsync.backend.repository.project;

import com.frietsync.backend.entity.project.ReleaseIssue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReleaseIssueRepository extends JpaRepository<ReleaseIssue, UUID> {
    List<ReleaseIssue> findByReleaseId(UUID releaseId);
    boolean existsByReleaseIdAndIssueId(UUID releaseId, UUID issueId);
    void deleteByReleaseId(UUID releaseId);
}
