package com.frietsync.backend.service.impl.gantt;

import com.frietsync.backend.dto.gantt.GanttBaselineResponse;
import com.frietsync.backend.dto.gantt.GanttCriticalPathResponse;
import com.frietsync.backend.dto.gantt.GanttItemResponse;
import com.frietsync.backend.dto.gantt.GanttLinkResponse;
import com.frietsync.backend.dto.gantt.GanttMoveRequest;
import com.frietsync.backend.dto.gantt.GanttMoveResponse;
import com.frietsync.backend.dto.gantt.GanttResponse;
import com.frietsync.backend.dto.issue.IssueAssigneeResponse;
import com.frietsync.backend.entity.gantt.GanttBaseline;
import com.frietsync.backend.entity.gantt.GanttItemType;
import com.frietsync.backend.entity.issue.Issue;
import com.frietsync.backend.entity.issue.IssueAssignee;
import com.frietsync.backend.entity.issue.IssueHistoryAction;
import com.frietsync.backend.entity.issue.IssueLink;
import com.frietsync.backend.entity.issue.IssueLinkType;
import com.frietsync.backend.entity.issue.IssueStatus;
import com.frietsync.backend.entity.project.Milestone;
import com.frietsync.backend.entity.project.Project;
import com.frietsync.backend.entity.project.Release;
import com.frietsync.backend.entity.project.ReleaseIssue;
import com.frietsync.backend.entity.sprint.Sprint;
import com.frietsync.backend.entity.sprint.SprintStatus;
import com.frietsync.backend.entity.user.Role;
import com.frietsync.backend.entity.user.User;
import com.frietsync.backend.exception.BadRequestException;
import com.frietsync.backend.exception.ResourceNotFoundException;
import com.frietsync.backend.repository.gantt.GanttBaselineRepository;
import com.frietsync.backend.repository.issue.IssueAssigneeRepository;
import com.frietsync.backend.repository.issue.IssueLinkRepository;
import com.frietsync.backend.repository.issue.IssueRepository;
import com.frietsync.backend.repository.project.MilestoneRepository;
import com.frietsync.backend.repository.project.ReleaseIssueRepository;
import com.frietsync.backend.repository.project.ReleaseRepository;
import com.frietsync.backend.repository.sprint.SprintRepository;
import com.frietsync.backend.repository.user.UserRepository;
import com.frietsync.backend.service.activity.ProjectActivityUpdatedEvent;
import com.frietsync.backend.service.gantt.GanttService;
import com.frietsync.backend.service.impl.issue.IssueHistoryRecorder;
import com.frietsync.backend.service.impl.project.ProjectAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GanttServiceImpl implements GanttService {

    private final ProjectAccess access;
    private final IssueRepository issueRepository;
    private final IssueLinkRepository issueLinkRepository;
    private final IssueAssigneeRepository assigneeRepository;
    private final SprintRepository sprintRepository;
    private final MilestoneRepository milestoneRepository;
    private final ReleaseRepository releaseRepository;
    private final ReleaseIssueRepository releaseIssueRepository;
    private final GanttBaselineRepository baselineRepository;
    private final UserRepository userRepository;
    private final IssueHistoryRecorder historyRecorder;
    private final ApplicationEventPublisher events;

    private static final LocalDate MIN_DATE = LocalDate.of(2000, 1, 1);
    private static final LocalDate MAX_DATE = LocalDate.of(2100, 12, 31);

    @Override
    @Transactional(readOnly = true)
    public GanttResponse getGantt(UUID projectId, UUID userId) {
        Project project = access.project(projectId);
        access.requireMember(project, userId);

        List<Issue> issues = issueRepository.findByProjectIdAndDeletedAtIsNullOrderByCreatedAtAsc(projectId);
        List<Sprint> sprints = sprintRepository.findByProjectId(projectId);
        List<Milestone> milestones = milestoneRepository.findByProjectIdOrderByDueDateAsc(projectId);
        List<Release> releases = releaseRepository.findByProjectIdOrderByReleaseDateAsc(projectId);

        Map<UUID, Issue> issueById = new HashMap<>();
        for (Issue issue : issues) {
            issueById.put(issue.getId(), issue);
        }

        List<IssueLink> links = loadLinks(projectId, issueById);
        CriticalPath path = calculateCriticalPath(issues, links);
        Map<String, GanttBaseline> baselines = loadBaselines(projectId);
        Map<UUID, List<IssueAssigneeResponse>> assignees = loadAssignees(issues);
        Set<UUID> criticalIds = new HashSet<>(path.issueIds);
        LocalDate today = LocalDate.now();

        GanttResponse response = new GanttResponse();
        response.setProjectId(project.getId());
        response.setProjectName(project.getName());
        response.setProjectStartDate(project.getStartDate());
        response.setProjectDeadline(project.getDeadline());
        response.setProjectedEndDate(path.projectedEnd);
        response.setCriticalPath(path.issueIds);
        response.setHasBaseline(!baselines.isEmpty());
        for (GanttBaseline baseline : baselines.values()) {
            response.setBaselineCapturedAt(baseline.getCreatedAt());
            break;
        }

        for (Sprint sprint : sprints) {
            response.getSprints().add(buildSprintItem(sprint, baselines, today));
        }
        for (Issue issue : issues) {
            response.getIssues().add(buildIssueItem(issue, baselines, assignees, criticalIds, today));
        }
        for (Milestone milestone : milestones) {
            response.getMilestones().add(buildMilestoneItem(milestone, issues, today));
        }
        for (Release release : releases) {
            response.getReleases().add(buildReleaseItem(release, issueById));
        }

        for (IssueLink link : links) {
            response.getLinks().add(buildLink(link, issueById, path));
        }

        calculateRange(response, project);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public GanttCriticalPathResponse getCriticalPath(UUID projectId, UUID userId) {
        Project project = access.project(projectId);
        access.requireMember(project, userId);

        List<Issue> issues = issueRepository.findByProjectIdAndDeletedAtIsNullOrderByCreatedAtAsc(projectId);
        Map<UUID, Issue> issueById = new HashMap<>();
        for (Issue issue : issues) {
            issueById.put(issue.getId(), issue);
        }

        List<IssueLink> links = loadLinks(projectId, issueById);
        CriticalPath path = calculateCriticalPath(issues, links);
        Map<String, GanttBaseline> baselines = loadBaselines(projectId);
        Map<UUID, List<IssueAssigneeResponse>> assignees = loadAssignees(issues);
        Set<UUID> criticalIds = new HashSet<>(path.issueIds);
        LocalDate today = LocalDate.now();

        GanttCriticalPathResponse response = new GanttCriticalPathResponse();
        response.setIssueIds(path.issueIds);
        response.setProjectedEndDate(path.projectedEnd);
        response.setProjectDeadline(project.getDeadline());

        for (UUID issueId : path.issueIds) {
            Issue issue = issueById.get(issueId);
            response.getIssues().add(buildIssueItem(issue, baselines, assignees, criticalIds, today));
        }

        if (path.projectedEnd != null && project.getDeadline() != null
                && path.projectedEnd.isAfter(project.getDeadline())) {
            long late = ChronoUnit.DAYS.between(project.getDeadline(), path.projectedEnd);
            response.setDaysLate((int) late);
        }
        return response;
    }

    @Override
    @Transactional
    public GanttMoveResponse moveIssue(UUID projectId, UUID issueId, GanttMoveRequest request, UUID userId) {
        Project project = access.project(projectId);
        access.require(project, userId, Role.PROJECT_MANAGER);
        access.requireActiveProject(project);
        validateDates(project, request);

        Issue issue = issueRepository.findByIdForUpdate(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found"));
        if (issue.getDeletedAt() != null || !issue.getProjectId().equals(projectId)) {
            throw new ResourceNotFoundException("Issue not found");
        }
        if (isFinished(issue)) {
            throw new BadRequestException("Resolved or closed issues cannot be rescheduled");
        }

        LocalDate oldStart = issue.getStartDate();
        LocalDate oldEnd = issue.getDueDate();
        issue.setStartDate(request.getStartDate());
        issue.setDueDate(request.getEndDate());
        issueRepository.save(issue);

        if (!Objects.equals(oldStart, request.getStartDate())) {
            historyRecorder.record(issueId, userId, IssueHistoryAction.UPDATED,
                    "startDate", oldStart, request.getStartDate());
        }
        if (!Objects.equals(oldEnd, request.getEndDate())) {
            historyRecorder.record(issueId, userId, IssueHistoryAction.UPDATED,
                    "dueDate", oldEnd, request.getEndDate());
        }

        GanttMoveResponse response = new GanttMoveResponse();
        response.setId(issue.getId());
        response.setType(GanttItemType.ISSUE.name());
        response.setStartDate(issue.getStartDate());
        response.setEndDate(issue.getDueDate());
        return response;
    }

    @Override
    @Transactional
    public GanttMoveResponse moveSprint(UUID projectId, UUID sprintId, GanttMoveRequest request, UUID userId) {
        Project project = access.project(projectId);
        access.require(project, userId, Role.PROJECT_MANAGER);
        access.requireActiveProject(project);
        validateDates(project, request);

        Sprint sprint = sprintRepository.findByIdForUpdate(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint not found"));
        if (!sprint.getProjectId().equals(projectId)) {
            throw new ResourceNotFoundException("Sprint not found");
        }
        if (sprint.getStatus() == SprintStatus.COMPLETED) {
            throw new BadRequestException("Completed sprints cannot be rescheduled");
        }

        sprint.setStartDate(request.getStartDate());
        sprint.setEndDate(request.getEndDate());
        sprintRepository.save(sprint);
        events.publishEvent(new ProjectActivityUpdatedEvent(projectId));

        GanttMoveResponse response = new GanttMoveResponse();
        response.setId(sprint.getId());
        response.setType(GanttItemType.SPRINT.name());
        response.setStartDate(sprint.getStartDate());
        response.setEndDate(sprint.getEndDate());
        return response;
    }

    private void validateDates(Project project, GanttMoveRequest request) {
        LocalDate start = request.getStartDate();
        LocalDate end = request.getEndDate();
        if (end.isBefore(start)) {
            throw new BadRequestException("End date cannot be before start date");
        }
        if (start.isBefore(MIN_DATE) || end.isAfter(MAX_DATE)) {
            throw new BadRequestException("Dates must be between the years 2000 and 2100");
        }
        if (project.getStartDate() != null && start.isBefore(project.getStartDate())) {
            throw new BadRequestException("Start date cannot be before the project start date");
        }
        if (project.getDeadline() != null && end.isAfter(project.getDeadline())) {
            throw new BadRequestException("End date cannot be after the project deadline");
        }
    }

    @Override
    @Transactional
    public GanttBaselineResponse setBaseline(UUID projectId, UUID userId) {
        Project project = access.project(projectId);
        access.require(project, userId, Role.PROJECT_MANAGER);
        access.requireActiveProject(project);

        List<GanttBaseline> rows = new ArrayList<>();
        int issueCount = 0;
        int sprintCount = 0;

        List<Issue> issues = issueRepository.findByProjectIdAndDeletedAtIsNull(projectId);
        for (Issue issue : issues) {
            if (issue.getStartDate() == null || issue.getDueDate() == null) {
                continue;
            }
            rows.add(newBaseline(projectId, GanttItemType.ISSUE, issue.getId(),
                    issue.getStartDate(), issue.getDueDate(), userId));
            issueCount++;
        }

        List<Sprint> sprints = sprintRepository.findByProjectId(projectId);
        for (Sprint sprint : sprints) {
            if (sprint.getStartDate() == null || sprint.getEndDate() == null) {
                continue;
            }
            rows.add(newBaseline(projectId, GanttItemType.SPRINT, sprint.getId(),
                    sprint.getStartDate(), sprint.getEndDate(), userId));
            sprintCount++;
        }

        if (rows.isEmpty()) {
            throw new BadRequestException("Nothing to save: no issue or sprint has both a start and an end date");
        }

        baselineRepository.deleteAllForProject(projectId);
        baselineRepository.saveAll(rows);

        GanttBaselineResponse response = new GanttBaselineResponse();
        response.setCapturedAt(Instant.now());
        response.setIssueCount(issueCount);
        response.setSprintCount(sprintCount);
        return response;
    }

    @Override
    @Transactional
    public void clearBaseline(UUID projectId, UUID userId) {
        Project project = access.project(projectId);
        access.require(project, userId, Role.PROJECT_MANAGER);
        access.requireActiveProject(project);
        baselineRepository.deleteAllForProject(projectId);
    }

    private GanttBaseline newBaseline(UUID projectId, GanttItemType type, UUID itemId,
                                      LocalDate start, LocalDate end, UUID userId) {
        GanttBaseline baseline = new GanttBaseline();
        baseline.setProjectId(projectId);
        baseline.setItemType(type);
        baseline.setItemId(itemId);
        baseline.setStartDate(start);
        baseline.setEndDate(end);
        baseline.setCreatedBy(userId);
        return baseline;
    }

    private List<IssueLink> loadLinks(UUID projectId, Map<UUID, Issue> issueById) {
        List<IssueLink> result = new ArrayList<>();
        List<IssueLink> all = issueLinkRepository.findByProjectId(projectId);
        for (IssueLink link : all) {

            if (issueById.containsKey(link.getSourceIssueId()) && issueById.containsKey(link.getTargetIssueId())) {
                result.add(link);
            }
        }
        return result;
    }

    private Map<String, GanttBaseline> loadBaselines(UUID projectId) {
        Map<String, GanttBaseline> result = new HashMap<>();
        List<GanttBaseline> rows = baselineRepository.findByProjectId(projectId);
        for (GanttBaseline row : rows) {
            result.put(baselineKey(row.getItemType(), row.getItemId()), row);
        }
        return result;
    }

    private String baselineKey(GanttItemType type, UUID id) {
        return type.name() + ":" + id;
    }

    private Map<UUID, List<IssueAssigneeResponse>> loadAssignees(List<Issue> issues) {
        Map<UUID, List<IssueAssigneeResponse>> result = new HashMap<>();
        if (issues.isEmpty()) {
            return result;
        }

        List<UUID> issueIds = new ArrayList<>();
        for (Issue issue : issues) {
            issueIds.add(issue.getId());
        }
        List<IssueAssignee> rows = assigneeRepository.findByIssueIdIn(issueIds);

        Set<UUID> userIds = new HashSet<>();
        for (IssueAssignee row : rows) {
            userIds.add(row.getUserId());
        }
        Map<UUID, String> names = new HashMap<>();
        for (User user : userRepository.findAllById(userIds)) {
            names.put(user.getId(), user.getName());
        }

        for (IssueAssignee row : rows) {
            List<IssueAssigneeResponse> list = result.get(row.getIssueId());
            if (list == null) {
                list = new ArrayList<>();
                result.put(row.getIssueId(), list);
            }
            list.add(new IssueAssigneeResponse(row.getUserId(), names.get(row.getUserId())));
        }
        return result;
    }

    private GanttItemResponse buildIssueItem(Issue issue, Map<String, GanttBaseline> baselines,
                                             Map<UUID, List<IssueAssigneeResponse>> assignees,
                                             Set<UUID> criticalIds, LocalDate today) {
        GanttItemResponse item = new GanttItemResponse();
        item.setId(issue.getId());
        item.setType(GanttItemType.ISSUE.name());
        item.setName(issue.getTitle());
        item.setIssueNumber(issue.getIssueNumber());
        item.setStatus(issue.getStatus().name());
        item.setPriority(issue.getPriority().name());
        item.setStartDate(issue.getStartDate());
        item.setEndDate(issue.getDueDate());
        item.setMilestoneId(issue.getMilestoneId());
        item.setCritical(criticalIds.contains(issue.getId()));

        List<IssueAssigneeResponse> list = assignees.get(issue.getId());
        if (list != null) {
            item.setAssignees(list);
        }

        GanttBaseline baseline = baselines.get(baselineKey(GanttItemType.ISSUE, issue.getId()));
        applyBaseline(item, baseline);

        boolean overdue = issue.getDueDate() != null && issue.getDueDate().isBefore(today)
                && !isFinished(issue);
        item.setOverdue(overdue);
        return item;
    }

    private GanttItemResponse buildSprintItem(Sprint sprint, Map<String, GanttBaseline> baselines,
                                              LocalDate today) {
        GanttItemResponse item = new GanttItemResponse();
        item.setId(sprint.getId());
        item.setType(GanttItemType.SPRINT.name());
        item.setName(sprint.getName());
        item.setStatus(sprint.getStatus().name());
        item.setStartDate(sprint.getStartDate());
        item.setEndDate(sprint.getEndDate());
        item.setLinkedIssueId(sprint.getIssueId());

        GanttBaseline baseline = baselines.get(baselineKey(GanttItemType.SPRINT, sprint.getId()));
        applyBaseline(item, baseline);

        boolean overdue = sprint.getEndDate() != null && sprint.getEndDate().isBefore(today)
                && sprint.getStatus() != SprintStatus.COMPLETED;
        item.setOverdue(overdue);
        return item;
    }

    private GanttItemResponse buildMilestoneItem(Milestone milestone, List<Issue> issues, LocalDate today) {
        GanttItemResponse item = new GanttItemResponse();
        item.setId(milestone.getId());
        item.setType(GanttItemType.MILESTONE.name());
        item.setName(milestone.getName());
        item.setStartDate(milestone.getDueDate());
        item.setEndDate(milestone.getDueDate());

        boolean completed = milestone.getCompletedAt() != null;
        item.setStatus(completed ? "COMPLETED" : "OPEN");
        item.setOverdue(!completed && milestone.getDueDate().isBefore(today));

        boolean atRisk = false;
        if (!completed) {
            for (Issue issue : issues) {
                if (milestone.getId().equals(issue.getMilestoneId()) && !isFinished(issue)
                        && issue.getDueDate() != null && issue.getDueDate().isAfter(milestone.getDueDate())) {
                    atRisk = true;
                    break;
                }
            }
        }
        item.setAtRisk(atRisk);
        return item;
    }

    private GanttItemResponse buildReleaseItem(Release release, Map<UUID, Issue> issueById) {
        GanttItemResponse item = new GanttItemResponse();
        item.setId(release.getId());
        item.setType(GanttItemType.RELEASE.name());
        item.setName(release.getName());
        item.setStartDate(release.getReleaseDate());
        item.setEndDate(release.getReleaseDate());

        boolean atRisk = false;
        if (release.getReleaseDate() != null) {
            List<ReleaseIssue> releaseIssues = releaseIssueRepository.findByReleaseId(release.getId());
            for (ReleaseIssue releaseIssue : releaseIssues) {
                Issue issue = issueById.get(releaseIssue.getIssueId());
                if (issue != null && !isFinished(issue) && issue.getDueDate() != null
                        && issue.getDueDate().isAfter(release.getReleaseDate())) {
                    atRisk = true;
                    break;
                }
            }
        }
        item.setAtRisk(atRisk);
        return item;
    }

    private void applyBaseline(GanttItemResponse item, GanttBaseline baseline) {
        if (baseline == null) {
            return;
        }
        item.setBaselineStartDate(baseline.getStartDate());
        item.setBaselineEndDate(baseline.getEndDate());
        if (item.getEndDate() != null) {
            long days = ChronoUnit.DAYS.between(baseline.getEndDate(), item.getEndDate());
            if (days > 0) {
                item.setDelayDays((int) days);
            }
        }
    }

    private GanttLinkResponse buildLink(IssueLink link, Map<UUID, Issue> issueById, CriticalPath path) {
        GanttLinkResponse response = new GanttLinkResponse();
        response.setId(link.getId());
        response.setSourceIssueId(link.getSourceIssueId());
        response.setTargetIssueId(link.getTargetIssueId());
        response.setType(link.getType().name());

        if (link.getType() == IssueLinkType.BLOCKS) {
            String key = link.getSourceIssueId() + ":" + link.getTargetIssueId();
            response.setCritical(path.linkKeys.contains(key));

            Issue source = issueById.get(link.getSourceIssueId());
            Issue target = issueById.get(link.getTargetIssueId());
            if (source != null && target != null && !isFinished(source)
                    && source.getDueDate() != null && target.getStartDate() != null
                    && !target.getStartDate().isAfter(source.getDueDate())) {
                response.setViolated(true);
            }
        }
        return response;
    }

    private void calculateRange(GanttResponse response, Project project) {
        LocalDate min = null;
        LocalDate max = null;

        List<GanttItemResponse> all = new ArrayList<>();
        all.addAll(response.getSprints());
        all.addAll(response.getIssues());
        all.addAll(response.getMilestones());
        all.addAll(response.getReleases());

        for (GanttItemResponse item : all) {
            LocalDate[] dates = {item.getStartDate(), item.getEndDate(),
                    item.getBaselineStartDate(), item.getBaselineEndDate()};
            for (LocalDate date : dates) {
                if (date == null) {
                    continue;
                }
                if (min == null || date.isBefore(min)) {
                    min = date;
                }
                if (max == null || date.isAfter(max)) {
                    max = date;
                }
            }
        }

        LocalDate[] projectDates = {project.getStartDate(), project.getDeadline(), response.getProjectedEndDate()};
        for (LocalDate date : projectDates) {
            if (date == null) {
                continue;
            }
            if (min == null || date.isBefore(min)) {
                min = date;
            }
            if (max == null || date.isAfter(max)) {
                max = date;
            }
        }

        response.setRangeStart(min);
        response.setRangeEnd(max);
    }

    private boolean isFinished(Issue issue) {
        return issue.getStatus() == IssueStatus.RESOLVED || issue.getStatus() == IssueStatus.CLOSED;
    }

    private static class CriticalPath {
        List<UUID> issueIds = new ArrayList<>();
        Set<String> linkKeys = new HashSet<>();
        LocalDate projectedEnd;
    }

    private CriticalPath calculateCriticalPath(List<Issue> issues, List<IssueLink> links) {
        CriticalPath path = new CriticalPath();

        Map<UUID, Issue> nodes = new HashMap<>();
        for (Issue issue : issues) {
            if (!isFinished(issue) && issue.getStartDate() != null && issue.getDueDate() != null) {
                nodes.put(issue.getId(), issue);
            }
        }
        if (nodes.isEmpty()) {
            return path;
        }

        Map<UUID, List<UUID>> blockers = new HashMap<>();
        for (IssueLink link : links) {
            if (link.getType() != IssueLinkType.BLOCKS) {
                continue;
            }
            if (!nodes.containsKey(link.getSourceIssueId()) || !nodes.containsKey(link.getTargetIssueId())) {
                continue;
            }
            List<UUID> list = blockers.get(link.getTargetIssueId());
            if (list == null) {
                list = new ArrayList<>();
                blockers.put(link.getTargetIssueId(), list);
            }
            list.add(link.getSourceIssueId());
        }

        Map<UUID, LocalDate> finish = new HashMap<>();
        Map<UUID, UUID> driver = new HashMap<>();
        Set<UUID> visiting = new HashSet<>();

        for (Issue issue : issues) {
            if (nodes.containsKey(issue.getId())) {
                calculateFinish(issue.getId(), nodes, blockers, finish, driver, visiting);
            }
        }

        UUID last = null;
        for (UUID id : finish.keySet()) {
            if (last == null) {
                last = id;
                continue;
            }
            LocalDate current = finish.get(id);
            LocalDate best = finish.get(last);
            if (current.isAfter(best)) {
                last = id;
            } else if (current.equals(best)
                    && nodes.get(id).getIssueNumber() < nodes.get(last).getIssueNumber()) {
                last = id;
            }
        }
        path.projectedEnd = finish.get(last);

        Set<UUID> seen = new HashSet<>();
        UUID current = last;
        while (current != null && !seen.contains(current)) {
            seen.add(current);
            path.issueIds.add(current);
            UUID previous = driver.get(current);
            if (previous != null) {
                path.linkKeys.add(previous + ":" + current);
            }
            current = previous;
        }
        Collections.reverse(path.issueIds);
        return path;
    }

    private LocalDate calculateFinish(UUID id, Map<UUID, Issue> nodes, Map<UUID, List<UUID>> blockers,
                                      Map<UUID, LocalDate> finish, Map<UUID, UUID> driver,
                                      Set<UUID> visiting) {
        if (finish.containsKey(id)) {
            return finish.get(id);
        }

        Issue issue = nodes.get(id);
        visiting.add(id);

        LocalDate start = issue.getStartDate();
        UUID startDriver = null;
        List<UUID> list = blockers.get(id);
        if (list != null) {
            for (UUID blockerId : list) {
                if (visiting.contains(blockerId)) {
                    continue;
                }
                LocalDate blockerFinish = calculateFinish(blockerId, nodes, blockers, finish, driver, visiting);
                LocalDate possibleStart = blockerFinish.plusDays(1);
                if (possibleStart.isAfter(start)) {
                    start = possibleStart;
                    startDriver = blockerId;
                }
            }
        }
        visiting.remove(id);

        long duration = ChronoUnit.DAYS.between(issue.getStartDate(), issue.getDueDate());
        if (duration < 0) {
            duration = 0;
        }
        LocalDate end = start.plusDays(duration);
        finish.put(id, end);
        if (startDriver != null) {
            driver.put(id, startDriver);
        }
        return end;
    }
}