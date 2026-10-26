package com.frietsync.backend.service.impl.issue;

import com.frietsync.backend.entity.issue.AssignmentRequestStatus;
import com.frietsync.backend.repository.issue.IssueAssignmentRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class IssueAssignmentRequestCleanupScheduler {

    private final IssueAssignmentRequestRepository requestRepository;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void deleteExpiredPendingRequests() {
        requestRepository.deleteExpiredPendingRequests(
                AssignmentRequestStatus.PENDING, Instant.now().minus(7, ChronoUnit.DAYS));
    }
}
