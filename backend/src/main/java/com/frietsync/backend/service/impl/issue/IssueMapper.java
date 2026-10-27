package com.frietsync.backend.service.impl.issue;

import com.frietsync.backend.dto.issue.IssueResponse;
import com.frietsync.backend.dto.issue.GitHubIssueResponse;
import com.frietsync.backend.dto.issue.IssueAssigneeResponse;
import com.frietsync.backend.entity.issue.Issue;
import com.frietsync.backend.entity.issue.IssueAssignee;
import com.frietsync.backend.repository.issue.IssueAssigneeRepository;
import com.frietsync.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class IssueMapper {

    private final IssueAssigneeRepository assigneeRepository;
    private final UserRepository userRepository;

    public IssueResponse toResponse(Issue issue) {
        return toResponses(List.of(issue)).get(0);
    }

    public List<IssueResponse> toResponses(List<Issue> issues) {
        if (issues.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<UUID>> assignees = assigneeRepository
                .findByIssueIdIn(issues.stream().map(Issue::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(IssueAssignee::getIssueId,
                        Collectors.mapping(IssueAssignee::getUserId, Collectors.toList())));
        Set<UUID> userIdSet = new LinkedHashSet<>();
        issues.stream().map(Issue::getCreatedBy).filter(Objects::nonNull).forEach(userIdSet::add);
        assignees.values().forEach(userIdSet::addAll);
        Map<UUID, String> names = new HashMap<>();
        userRepository.findAllById(userIdSet).forEach(user -> names.put(user.getId(), user.getName()));
        return issues.stream()
                .map(i -> build(i, assignees.getOrDefault(i.getId(), List.of()), names))
                .toList();
    }

    private IssueResponse build(Issue i, List<UUID> assigneeIds, Map<UUID, String> names) {
        return IssueResponse.builder()
                .id(i.getId())
                .projectId(i.getProjectId())
                .issueNumber(i.getIssueNumber())
                .title(i.getTitle())
                .description(i.getDescription())
                .status(i.getStatus())
                .priority(i.getPriority())
                .startDate(i.getStartDate())
                .dueDate(i.getDueDate())
                .milestoneId(i.getMilestoneId())
                .estimatedHours(i.getEstimatedHours())
                .createdBy(i.getCreatedBy())
                .createdByName(names.get(i.getCreatedBy()))
                .assigneeIds(assigneeIds)
                .assignees(assigneeIds.stream()
                        .map(id -> new IssueAssigneeResponse(id, names.get(id)))
                        .toList())
                .resolvedAt(i.getResolvedAt())
                .closedAt(i.getClosedAt())
                .createdAt(i.getCreatedAt())
                .updatedAt(i.getUpdatedAt())
                .githubIssue(i.getGithubIssueNumber() == null ? null : GitHubIssueResponse.builder()
                        .number(i.getGithubIssueNumber())
                        .title(i.getGithubIssueTitle())
                        .url(i.getGithubIssueUrl())
                        .repository(i.getGithubIssueRepository())
                        .build())
                .build();
    }
}
