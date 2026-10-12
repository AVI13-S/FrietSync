package com.frietsync.backend.service.impl.issue;

import com.frietsync.backend.dto.issue.DependencyGraphResponse;
import com.frietsync.backend.dto.issue.IssueImpactResponse;
import com.frietsync.backend.dto.issue.IssueLinkRequest;
import com.frietsync.backend.dto.issue.IssueLinkResponse;
import com.frietsync.backend.dto.issue.IssueSummaryResponse;
import com.frietsync.backend.entity.issue.Issue;
import com.frietsync.backend.entity.issue.IssueHistoryAction;
import com.frietsync.backend.entity.issue.IssueLink;
import com.frietsync.backend.entity.issue.IssueLinkType;
import com.frietsync.backend.entity.project.Project;
import com.frietsync.backend.entity.user.Role;
import com.frietsync.backend.exception.BadRequestException;
import com.frietsync.backend.exception.ResourceNotFoundException;
import com.frietsync.backend.repository.issue.IssueLinkRepository;
import com.frietsync.backend.repository.issue.IssueRepository;
import com.frietsync.backend.service.impl.project.ProjectAccess;
import com.frietsync.backend.service.issue.IssueLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class IssueLinkServiceImpl implements IssueLinkService {

    private final IssueRepository issueRepository;
    private final IssueLinkRepository linkRepository;
    private final ProjectAccess access;
    private final IssueHistoryRecorder history;

    @Override
    public IssueLinkResponse create(UUID issueId, IssueLinkRequest r, UUID userId) {
        if (issueId.equals(r.getTargetIssueId())) {
            throw new BadRequestException("An issue cannot be linked to itself");
        }

        Issue source = access.issue(issueId);
        Project project = access.project(source.getProjectId());
        access.require(project, userId, Role.PROJECT_MANAGER, Role.TEAM_LEAD, Role.CONTRIBUTOR);

        Issue target = access.issue(r.getTargetIssueId());
        if (!target.getProjectId().equals(source.getProjectId())) {
            throw new BadRequestException("Both issues must belong to the same project");
        }

        IssueLinkType type = r.getType();
        boolean exists = linkRepository.existsBySourceIssueIdAndTargetIssueIdAndType(issueId, target.getId(), type);
        if (type == IssueLinkType.RELATES_TO
                && linkRepository.existsBySourceIssueIdAndTargetIssueIdAndType(target.getId(), issueId, type)) {
            exists = true;
        }
        if (exists) {
            throw new BadRequestException("These issues are already linked this way");
        }

        if (type == IssueLinkType.BLOCKS) {
            Map<UUID, List<UUID>> graph = blockGraph(linkRepository.findByProjectId(source.getProjectId()));
            if (downstream(graph, target.getId()).contains(issueId)) {
                throw new BadRequestException("This link would create a circular dependency");
            }
        }

        IssueLink link = new IssueLink();
        link.setProjectId(source.getProjectId());
        link.setSourceIssueId(issueId);
        link.setTargetIssueId(target.getId());
        link.setType(type);
        link.setCreatedBy(userId);
        linkRepository.save(link);

        history.record(issueId, userId, IssueHistoryAction.LINK_ADDED,
                "link", null, type + " #" + target.getIssueNumber());
        return toLinkResponses(List.of(link)).get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IssueLinkResponse> list(UUID issueId, UUID userId) {
        Issue issue = access.issue(issueId);
        access.requireMember(access.project(issue.getProjectId()), userId);
        return toLinkResponses(linkRepository
                .findBySourceIssueIdOrTargetIssueIdOrderByCreatedAtDesc(issueId, issueId));
    }

    @Override
    public void delete(UUID linkId, UUID userId) {
        IssueLink link = linkRepository.findById(linkId)
                .orElseThrow(() -> new ResourceNotFoundException("Link not found"));
        Issue source = access.issue(link.getSourceIssueId());
        Project project = access.project(source.getProjectId());
        access.require(project, userId, Role.PROJECT_MANAGER, Role.TEAM_LEAD, Role.CONTRIBUTOR);

        String oldValue = link.getType() + "";
        Issue target = issueRepository.findById(link.getTargetIssueId()).orElse(null);
        if (target != null) {
            oldValue = oldValue + " #" + target.getIssueNumber();
        }

        linkRepository.delete(link);
        history.record(source.getId(), userId, IssueHistoryAction.LINK_REMOVED,
                "link", oldValue, null);
    }

    @Override
    @Transactional(readOnly = true)
    public IssueImpactResponse impact(UUID issueId, UUID userId) {
        Issue issue = access.issue(issueId);
        access.requireMember(access.project(issue.getProjectId()), userId);

        List<IssueLink> links = linkRepository.findByProjectId(issue.getProjectId());
        Map<UUID, List<UUID>> graph = blockGraph(links);

        Set<UUID> allDependents = downstream(graph, issueId);
        List<UUID> direct = graph.get(issueId);
        if (direct == null) {
            direct = new ArrayList<>();
        }
        Set<UUID> indirect = new LinkedHashSet<>(allDependents);
        indirect.removeAll(direct);

        List<UUID> blockedBy = new ArrayList<>();
        List<UUID> related = new ArrayList<>();
        for (IssueLink link : links) {
            if (link.getType() == IssueLinkType.BLOCKS) {
                if (link.getTargetIssueId().equals(issueId)) {
                    blockedBy.add(link.getSourceIssueId());
                }
            } else {
                if (link.getSourceIssueId().equals(issueId)) {
                    related.add(link.getTargetIssueId());
                } else if (link.getTargetIssueId().equals(issueId)) {
                    related.add(link.getSourceIssueId());
                }
            }
        }

        return IssueImpactResponse.builder()
                .issue(toSummary(issue))
                .blockedBy(summaries(blockedBy))
                .directDependents(summaries(direct))
                .indirectDependents(summaries(indirect))
                .related(summaries(related))
                .totalAffected(allDependents.size())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IssueSummaryResponse> dependencies(UUID issueId, UUID userId) {
        Issue issue = access.issue(issueId);
        access.requireMember(access.project(issue.getProjectId()), userId);

        List<UUID> dependentIds = new ArrayList<>();
        for (IssueLink link : linkRepository.findByProjectId(issue.getProjectId())) {
            if (link.getType() == IssueLinkType.BLOCKS && link.getSourceIssueId().equals(issueId)) {
                dependentIds.add(link.getTargetIssueId());
            }
        }
        return summaries(dependentIds);
    }

    @Override
    @Transactional(readOnly = true)
    public DependencyGraphResponse dependencyGraph(UUID projectId, UUID userId) {
        Project project = access.project(projectId);
        access.requireMember(project, userId);

        List<IssueSummaryResponse> nodes = new ArrayList<>();
        for (Issue issue : issueRepository.findByProjectIdOrderByCreatedAtDesc(projectId)) {
            nodes.add(toSummary(issue));
        }
        List<IssueLinkResponse> edges = toLinkResponses(linkRepository.findByProjectId(projectId));

        return DependencyGraphResponse.builder()
                .nodes(nodes)
                .edges(edges)
                .build();
    }

    // Only BLOCKS links are used for the graph: source issue -> issues it blocks
    private Map<UUID, List<UUID>> blockGraph(List<IssueLink> links) {
        Map<UUID, List<UUID>> graph = new HashMap<>();
        for (IssueLink link : links) {
            if (link.getType() != IssueLinkType.BLOCKS) {
                continue;
            }
            List<UUID> blocked = graph.get(link.getSourceIssueId());
            if (blocked == null) {
                blocked = new ArrayList<>();
                graph.put(link.getSourceIssueId(), blocked);
            }
            blocked.add(link.getTargetIssueId());
        }
        return graph;
    }

    // Every issue reachable from start (direct and indirect), nearest first
    private Set<UUID> downstream(Map<UUID, List<UUID>> graph, UUID start) {
        Set<UUID> seen = new LinkedHashSet<>();
        Deque<UUID> queue = new ArrayDeque<>();
        queue.add(start);
        while (!queue.isEmpty()) {
            UUID current = queue.poll();
            List<UUID> next = graph.get(current);
            if (next == null) {
                continue;
            }
            for (UUID id : next) {
                if (seen.add(id)) {
                    queue.add(id);
                }
            }
        }
        return seen;
    }

    private List<IssueSummaryResponse> summaries(Collection<UUID> ids) {
        List<IssueSummaryResponse> result = new ArrayList<>();
        if (ids.isEmpty()) {
            return result;
        }
        Map<UUID, Issue> issues = new HashMap<>();
        for (Issue issue : issueRepository.findAllById(ids)) {
            issues.put(issue.getId(), issue);
        }
        for (UUID id : ids) {
            Issue issue = issues.get(id);
            if (issue != null) {
                result.add(toSummary(issue));
            }
        }
        return result;
    }

    private List<IssueLinkResponse> toLinkResponses(List<IssueLink> links) {
        Set<UUID> issueIds = new LinkedHashSet<>();
        for (IssueLink link : links) {
            issueIds.add(link.getSourceIssueId());
            issueIds.add(link.getTargetIssueId());
        }
        Map<UUID, Issue> issues = new HashMap<>();
        for (Issue issue : issueRepository.findAllById(issueIds)) {
            issues.put(issue.getId(), issue);
        }

        List<IssueLinkResponse> result = new ArrayList<>();
        for (IssueLink link : links) {
            Issue source = issues.get(link.getSourceIssueId());
            Issue target = issues.get(link.getTargetIssueId());
            result.add(IssueLinkResponse.builder()
                    .id(link.getId())
                    .projectId(link.getProjectId())
                    .sourceIssueId(link.getSourceIssueId())
                    .sourceIssueNumber(source == null ? 0 : source.getIssueNumber())
                    .sourceTitle(source == null ? null : source.getTitle())
                    .targetIssueId(link.getTargetIssueId())
                    .targetIssueNumber(target == null ? 0 : target.getIssueNumber())
                    .targetTitle(target == null ? null : target.getTitle())
                    .type(link.getType())
                    .createdBy(link.getCreatedBy())
                    .createdAt(link.getCreatedAt())
                    .build());
        }
        return result;
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
}