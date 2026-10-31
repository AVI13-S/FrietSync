package com.frietsync.backend.service.impl.issue;

import com.frietsync.backend.dto.issue.IssueAssigneeResponse;
import com.frietsync.backend.dto.issue.IssueChecklistItemResponse;
import com.frietsync.backend.dto.issue.IssueChecklistResponse;
import com.frietsync.backend.entity.issue.AssignmentRequestStatus;
import com.frietsync.backend.entity.issue.Issue;
import com.frietsync.backend.entity.issue.IssueAssignee;
import com.frietsync.backend.entity.issue.IssueLink;
import com.frietsync.backend.entity.issue.IssueLinkType;
import com.frietsync.backend.entity.issue.IssuePriority;
import com.frietsync.backend.entity.issue.IssueStatus;
import com.frietsync.backend.entity.project.Project;
import com.frietsync.backend.repository.issue.IssueAssigneeRepository;
import com.frietsync.backend.repository.issue.IssueAssignmentRequestRepository;
import com.frietsync.backend.repository.issue.IssueLinkRepository;
import com.frietsync.backend.repository.issue.IssueRepository;
import com.frietsync.backend.repository.user.UserRepository;
import com.frietsync.backend.service.impl.project.ProjectAccess;
import com.frietsync.backend.service.issue.IssueChecklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IssueChecklistServiceImpl implements IssueChecklistService {

    private static final int DUE_SOON_DAYS = 3;

    private final IssueRepository issueRepository;
    private final IssueAssigneeRepository assigneeRepository;
    private final IssueLinkRepository linkRepository;
    private final IssueAssignmentRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final ProjectAccess access;

    @Override
    public IssueChecklistResponse checklist(UUID projectId, UUID userId) {
        Project project = access.project(projectId);
        access.requireMember(project, userId);

        LocalDate today = LocalDate.now();
        List<Issue> issues = issueRepository.findByProjectIdAndDeletedAtIsNullOrderByCreatedAtAsc(projectId);

        Map<UUID, Issue> byId = new HashMap<>();
        for (Issue issue : issues) {
            byId.put(issue.getId(), issue);
        }

        Map<UUID, List<UUID>> assigneeIds = new HashMap<>();
        if (!byId.isEmpty()) {
            for (IssueAssignee assignee : assigneeRepository.findByIssueIdIn(byId.keySet())) {
                assigneeIds.computeIfAbsent(assignee.getIssueId(), k -> new ArrayList<>())
                        .add(assignee.getUserId());
            }
        }
        Set<UUID> userIds = new HashSet<>();
        assigneeIds.values().forEach(userIds::addAll);
        Map<UUID, String> names = new HashMap<>();
        if (!userIds.isEmpty()) {
            userRepository.findAllById(userIds).forEach(user -> names.put(user.getId(), user.getName()));
        }

        Map<UUID, List<Integer>> openBlockers = new HashMap<>();
        for (IssueLink link : linkRepository.findByProjectId(projectId)) {
            if (link.getType() != IssueLinkType.BLOCKS) {
                continue;
            }
            Issue blocker = byId.get(link.getSourceIssueId());
            if (blocker == null || blocker.getStatus() == IssueStatus.CLOSED) {
                continue;
            }
            openBlockers.computeIfAbsent(link.getTargetIssueId(), k -> new ArrayList<>())
                    .add(blocker.getIssueNumber());
        }

        Map<IssueStatus, Long> statusCounts = new EnumMap<>(IssueStatus.class);
        for (IssueStatus status : IssueStatus.values()) {
            statusCounts.put(status, 0L);
        }

        List<IssueChecklistItemResponse> completed = new ArrayList<>();
        List<IssueChecklistItemResponse> inReview = new ArrayList<>();
        List<IssueChecklistItemResponse> remaining = new ArrayList<>();
        int overdueCount = 0;
        int dueSoonCount = 0;
        int unassignedCount = 0;
        int blockedCount = 0;
        int highPriorityRemaining = 0;
        int reopenedCount = 0;
        int assignedToMeRemaining = 0;

        for (Issue issue : issues) {
            IssueStatus status = issue.getStatus();
            statusCounts.merge(status, 1L, Long::sum);

            boolean closed = status == IssueStatus.CLOSED;
            boolean review = status == IssueStatus.RESOLVED;
            boolean isRemaining = !closed && !review;

            List<UUID> ids = assigneeIds.getOrDefault(issue.getId(), List.of());
            List<Integer> blockers = isRemaining
                    ? openBlockers.getOrDefault(issue.getId(), List.of()) : List.of();
            LocalDate due = issue.getDueDate();

            boolean overdue = isRemaining && due != null && due.isBefore(today);
            boolean dueSoon = isRemaining && due != null && !due.isBefore(today)
                    && !due.isAfter(today.plusDays(DUE_SOON_DAYS));
            boolean unassigned = isRemaining && ids.isEmpty();
            boolean blocked = !blockers.isEmpty();

            IssueChecklistItemResponse item = IssueChecklistItemResponse.builder()
                    .id(issue.getId())
                    .issueNumber(issue.getIssueNumber())
                    .title(issue.getTitle())
                    .status(status)
                    .priority(issue.getPriority())
                    .dueDate(due)
                    .checked(closed)
                    .overdue(overdue)
                    .dueSoon(dueSoon)
                    .unassigned(unassigned)
                    .blocked(blocked)
                    .blockedByIssueNumbers(blockers)
                    .assignees(ids.stream()
                            .map(id -> new IssueAssigneeResponse(id, names.get(id)))
                            .toList())
                    .resolvedAt(issue.getResolvedAt())
                    .closedAt(issue.getClosedAt())
                    .build();

            if (closed) {
                completed.add(item);
            } else if (review) {
                inReview.add(item);
            } else {
                remaining.add(item);
                if (overdue) overdueCount++;
                if (dueSoon) dueSoonCount++;
                if (unassigned) unassignedCount++;
                if (blocked) blockedCount++;
                if (issue.getPriority() == IssuePriority.HIGH
                        || issue.getPriority() == IssuePriority.CRITICAL) {
                    highPriorityRemaining++;
                }
                if (status == IssueStatus.REOPENED) reopenedCount++;
                if (ids.contains(userId)) assignedToMeRemaining++;
            }
        }

        completed.sort(Comparator.comparing(IssueChecklistItemResponse::getClosedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        inReview.sort(Comparator.comparing(IssueChecklistItemResponse::getResolvedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        remaining.sort(Comparator
                .comparing(IssueChecklistItemResponse::getPriority, Comparator.reverseOrder())
                .thenComparing(IssueChecklistItemResponse::getDueDate,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparingInt(IssueChecklistItemResponse::getIssueNumber));

        int total = issues.size();
        int pendingRequests = requestRepository
                .findByProjectIdAndStatusOrderByCreatedAtAsc(projectId, AssignmentRequestStatus.PENDING)
                .size();

        return IssueChecklistResponse.builder()
                .projectId(projectId)
                .totalIssues(total)
                .completedCount(completed.size())
                .inReviewCount(inReview.size())
                .remainingCount(remaining.size())
                .completionPercent(total == 0 ? 0 : (int) Math.round(completed.size() * 100.0 / total))
                .statusCounts(statusCounts)
                .overdueCount(overdueCount)
                .dueSoonCount(dueSoonCount)
                .unassignedCount(unassignedCount)
                .blockedCount(blockedCount)
                .highPriorityRemainingCount(highPriorityRemaining)
                .reopenedCount(reopenedCount)
                .assignedToMeRemainingCount(assignedToMeRemaining)
                .pendingAssignmentRequests(pendingRequests)
                .completed(completed)
                .inReview(inReview)
                .remaining(remaining)
                .build();
    }
}