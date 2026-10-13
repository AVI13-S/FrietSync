package com.frietsync.backend.service.impl.activity;

import com.frietsync.backend.dto.activity.ActivityResponse;
import com.frietsync.backend.dto.activity.DelayedIssueResponse;
import com.frietsync.backend.dto.activity.PresenceResponse;
import com.frietsync.backend.dto.activity.ProjectSummaryResponse;
import com.frietsync.backend.dto.issue.IssueSummaryResponse;
import com.frietsync.backend.entity.issue.Issue;
import com.frietsync.backend.entity.issue.IssueAssignee;
import com.frietsync.backend.entity.issue.IssueHistory;
import com.frietsync.backend.entity.issue.IssueHistoryAction;
import com.frietsync.backend.entity.issue.IssueStatus;
import com.frietsync.backend.entity.project.Project;
import com.frietsync.backend.entity.user.Role;
import com.frietsync.backend.entity.user.User;
import com.frietsync.backend.repository.issue.IssueAssigneeRepository;
import com.frietsync.backend.repository.issue.IssueHistoryRepository;
import com.frietsync.backend.repository.issue.IssueRepository;
import com.frietsync.backend.repository.user.UserRepository;
import com.frietsync.backend.service.activity.ActivityService;
import com.frietsync.backend.service.impl.project.ProjectAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private final IssueRepository issueRepository;
    private final IssueAssigneeRepository assigneeRepository;
    private final IssueHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final ProjectAccess access;
    private final ProjectSummaryPdfBuilder pdfBuilder;

    @Override
    public List<PresenceResponse> presence(UUID projectId, UUID userId) {
        Project project = access.project(projectId);
        access.requireMember(project, userId);

        Map<UUID, Issue> inProgress = new LinkedHashMap<>();
        for (Issue issue : issueRepository.findByProjectId(projectId)) {
            if (issue.getStatus() == IssueStatus.IN_PROGRESS) {
                inProgress.put(issue.getId(), issue);
            }
        }
        List<PresenceResponse> result = new ArrayList<>();
        if (inProgress.isEmpty()) {
            return result;
        }

        // user id -> issues they are working on
        Map<UUID, List<IssueSummaryResponse>> work = new LinkedHashMap<>();
        for (IssueAssignee a : assigneeRepository.findByIssueIdIn(inProgress.keySet())) {
            List<IssueSummaryResponse> list = work.get(a.getUserId());
            if (list == null) {
                list = new ArrayList<>();
                work.put(a.getUserId(), list);
            }
            list.add(toSummary(inProgress.get(a.getIssueId())));
        }

        Map<UUID, User> users = new HashMap<>();
        for (User u : userRepository.findAllById(work.keySet())) {
            users.put(u.getId(), u);
        }
        for (UUID uid : work.keySet()) {
            User u = users.get(uid);
            result.add(PresenceResponse.builder()
                    .userId(uid)
                    .name(u == null ? null : u.getName())
                    .email(u == null ? null : u.getEmail())
                    .workingOn(work.get(uid))
                    .build());
        }
        return result;
    }

    @Override
    public List<ActivityResponse> activity(UUID projectId, int limit, UUID userId) {
        Project project = access.project(projectId);
        access.requireMember(project, userId);

        if (limit < 1) {
            limit = 50;
        }
        if (limit > 100) {
            limit = 100;
        }

        Map<UUID, Issue> issues = issuesOf(projectId);
        if (issues.isEmpty()) {
            return new ArrayList<>();
        }
        List<IssueHistory> entries = historyRepository
                .findByIssueIdInOrderByCreatedAtDesc(issues.keySet(), PageRequest.of(0, limit));
        return toActivityResponses(entries, issues);
    }

    @Override
    public List<ActivityResponse> events(UUID projectId, IssueHistoryAction action, Instant from, Instant to,
                                         int limit, UUID userId) {
        Project project = access.project(projectId);
        access.requireMember(project, userId);

        if (limit < 1) {
            limit = 200;
        }
        if (limit > 1000) {
            limit = 1000;
        }
        Instant start = from == null ? Instant.EPOCH : from;
        Instant end = to == null ? Instant.now() : to;

        Map<UUID, Issue> issues = issuesOf(projectId);
        if (issues.isEmpty()) {
            return new ArrayList<>();
        }
        List<IssueHistory> all = historyRepository
                .findByIssueIdInAndCreatedAtBetweenOrderByCreatedAtDesc(issues.keySet(), start, end);

        List<IssueHistory> picked = new ArrayList<>();
        for (IssueHistory h : all) {
            if (action != null && h.getAction() != action) {
                continue;
            }
            picked.add(h);
            if (picked.size() == limit) {
                break;
            }
        }
        return toActivityResponses(picked, issues);
    }

    @Override
    public ProjectSummaryResponse summary(UUID projectId, UUID userId) {
        Project project = access.project(projectId);
        access.requireMember(project, userId);
        return buildSummary(projectId);
    }

    @Override
    public byte[] summaryPdf(UUID projectId, UUID userId) {
        Project project = access.project(projectId);
        access.require(project, userId, Role.PROJECT_MANAGER, Role.TEAM_LEAD);
        return pdfBuilder.build(buildSummary(projectId));
    }

    private ProjectSummaryResponse buildSummary(UUID projectId) {
        LocalDate today = LocalDate.now();
        int open = 0;
        int inProgress = 0;
        int resolved = 0;
        int reopened = 0;
        int closed = 0;
        int overdue = 0;
        double estimatedTotal = 0;
        double estimatedDone = 0;
        long totalDays = 0;
        int resolvedCount = 0;
        List<DelayedIssueResponse> delayed = new ArrayList<>();

        List<Issue> issues = issueRepository.findByProjectId(projectId);
        for (Issue i : issues) {
            IssueStatus s = i.getStatus();
            boolean done = s == IssueStatus.RESOLVED || s == IssueStatus.CLOSED;

            if (s == IssueStatus.OPEN) {
                open++;
            } else if (s == IssueStatus.IN_PROGRESS) {
                inProgress++;
            } else if (s == IssueStatus.RESOLVED) {
                resolved++;
            } else if (s == IssueStatus.REOPENED) {
                reopened++;
            } else if (s == IssueStatus.CLOSED) {
                closed++;
            }

            double hours = i.getEstimatedHours() == null ? 0 : i.getEstimatedHours();
            estimatedTotal += hours;
            if (done) {
                estimatedDone += hours;
            }

            if (done && i.getResolvedAt() != null) {
                totalDays += ChronoUnit.DAYS.between(i.getCreatedAt(), i.getResolvedAt());
                resolvedCount++;
            }

            LocalDate due = i.getDueDate();
            if (due == null) {
                continue;
            }
            if (!done && due.isBefore(today)) {
                overdue++;
                delayed.add(toDelayed(i, (int) ChronoUnit.DAYS.between(due, today), false));
            } else if (done && i.getResolvedAt() != null) {
                LocalDate resolvedDate = i.getResolvedAt().atZone(ZoneId.systemDefault()).toLocalDate();
                if (resolvedDate.isAfter(due)) {
                    delayed.add(toDelayed(i, (int) ChronoUnit.DAYS.between(due, resolvedDate), true));
                }
            }
        }

        delayed.sort((a, b) -> b.getDaysLate() - a.getDaysLate());

        int total = issues.size();
        double completion = total == 0 ? 0 : round((resolved + closed) * 100.0 / total);
        Double average = resolvedCount == 0 ? null : round((double) totalDays / resolvedCount);

        return ProjectSummaryResponse.builder()
                .projectId(projectId)
                .totalIssues(total)
                .openIssues(open)
                .inProgressIssues(inProgress)
                .resolvedIssues(resolved)
                .reopenedIssues(reopened)
                .closedIssues(closed)
                .completionPercent(completion)
                .estimatedHoursTotal(estimatedTotal)
                .estimatedHoursCompleted(estimatedDone)
                .estimatedHoursRemaining(estimatedTotal - estimatedDone)
                .averageDaysToResolve(average)
                .overdueIssues(overdue)
                .delayedIssues(delayed)
                .build();
    }

    private Map<UUID, Issue> issuesOf(UUID projectId) {
        Map<UUID, Issue> issues = new HashMap<>();
        for (Issue issue : issueRepository.findByProjectId(projectId)) {
            issues.put(issue.getId(), issue);
        }
        return issues;
    }

    private List<ActivityResponse> toActivityResponses(List<IssueHistory> entries, Map<UUID, Issue> issues) {
        List<UUID> actorIds = new ArrayList<>();
        for (IssueHistory h : entries) {
            if (!actorIds.contains(h.getActorId())) {
                actorIds.add(h.getActorId());
            }
        }
        Map<UUID, User> users = new HashMap<>();
        for (User u : userRepository.findAllById(actorIds)) {
            users.put(u.getId(), u);
        }

        List<ActivityResponse> result = new ArrayList<>();
        for (IssueHistory h : entries) {
            Issue issue = issues.get(h.getIssueId());
            User actor = users.get(h.getActorId());
            result.add(ActivityResponse.builder()
                    .id(h.getId())
                    .issueId(h.getIssueId())
                    .issueNumber(issue == null ? 0 : issue.getIssueNumber())
                    .issueTitle(issue == null ? null : issue.getTitle())
                    .actorId(h.getActorId())
                    .actorName(actor == null ? null : actor.getName())
                    .action(h.getAction())
                    .field(h.getFieldName())
                    .oldValue(h.getOldValue())
                    .newValue(h.getNewValue())
                    .comment(h.getComment())
                    .createdAt(h.getCreatedAt())
                    .build());
        }
        return result;
    }

    private DelayedIssueResponse toDelayed(Issue i, int daysLate, boolean resolvedLate) {
        return DelayedIssueResponse.builder()
                .id(i.getId())
                .issueNumber(i.getIssueNumber())
                .title(i.getTitle())
                .status(i.getStatus())
                .dueDate(i.getDueDate())
                .daysLate(daysLate)
                .resolvedLate(resolvedLate)
                .build();
    }

    private IssueSummaryResponse toSummary(Issue i) {
        return IssueSummaryResponse.builder()
                .id(i.getId())
                .issueNumber(i.getIssueNumber())
                .title(i.getTitle())
                .status(i.getStatus())
                .priority(i.getPriority())
                .dueDate(i.getDueDate())
                .build();
    }

    private double round(double value) {
        return Math.round(value * 10) / 10.0;
    }
}