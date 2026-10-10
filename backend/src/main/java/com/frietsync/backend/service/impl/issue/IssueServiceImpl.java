package com.frietsync.backend.service.impl.issue;

import com.frietsync.backend.dto.issue.IssueHistoryResponse;
import com.frietsync.backend.dto.issue.IssueLabelRequest;
import com.frietsync.backend.dto.issue.IssueRequest;
import com.frietsync.backend.dto.issue.IssueResponse;
import com.frietsync.backend.dto.issue.IssueUpdateRequest;
import com.frietsync.backend.dto.issue.ReviewRequest;
import com.frietsync.backend.dto.project.LabelResponse;
import com.frietsync.backend.entity.issue.Issue;
import com.frietsync.backend.entity.issue.IssueHistoryAction;
import com.frietsync.backend.entity.issue.IssueLabel;
import com.frietsync.backend.entity.issue.IssuePriority;
import com.frietsync.backend.entity.issue.IssueStatus;
import com.frietsync.backend.entity.project.Label;
import com.frietsync.backend.entity.project.Project;
import com.frietsync.backend.entity.user.Role;
import com.frietsync.backend.exception.BadRequestException;
import com.frietsync.backend.exception.ForbiddenException;
import com.frietsync.backend.exception.ResourceNotFoundException;
import com.frietsync.backend.repository.issue.IssueAssigneeRepository;
import com.frietsync.backend.repository.issue.IssueAssignmentRequestRepository;
import com.frietsync.backend.repository.issue.IssueHistoryRepository;
import com.frietsync.backend.repository.issue.IssueLabelRepository;
import com.frietsync.backend.repository.issue.IssueRepository;
import com.frietsync.backend.repository.project.LabelRepository;
import com.frietsync.backend.repository.project.ProjectRepository;
import com.frietsync.backend.service.impl.project.ProjectAccess;
import com.frietsync.backend.service.issue.IssueAttachmentService;
import com.frietsync.backend.service.issue.IssueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class IssueServiceImpl implements IssueService {

    private final IssueRepository issueRepository;
    private final IssueAssigneeRepository assigneeRepository;
    private final IssueAssignmentRequestRepository requestRepository;
    private final IssueHistoryRepository historyRepository;
    private final IssueLabelRepository issueLabelRepository;
    private final LabelRepository labelRepository;
    private final ProjectRepository projectRepository;
    private final IssueAttachmentService attachmentService;
    private final ProjectAccess access;
    private final IssueMapper mapper;
    private final IssueHistoryRecorder history;

    @Override
    public IssueResponse create(UUID projectId, IssueRequest r, UUID userId) {
        Project project = projectRepository.findByIdForUpdate(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        access.requireMember(project, userId);
        project.setIssueCounter(project.getIssueCounter() + 1);
        projectRepository.save(project);

        Issue issue = new Issue();
        issue.setProjectId(projectId);
        issue.setIssueNumber(project.getIssueCounter());
        issue.setTitle(r.getTitle().trim());
        issue.setDescription(r.getDescription());
        issue.setPriority(r.getPriority() != null ? r.getPriority() : IssuePriority.MEDIUM);
        issue.setDueDate(r.getDueDate());
        issue.setEstimatedHours(r.getEstimatedHours());
        issue.setCreatedBy(userId);
        issueRepository.save(issue);

        history.record(issue.getId(), userId, IssueHistoryAction.CREATED);
        return mapper.toResponse(issue);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IssueResponse> listForProject(UUID projectId, UUID userId) {
        Project project = access.project(projectId);
        access.requireMember(project, userId);
        return mapper.toResponses(issueRepository.findByProjectIdOrderByCreatedAtDesc(projectId));
    }

    @Override
    @Transactional(readOnly = true)
    public IssueResponse get(UUID issueId, UUID userId) {
        Issue issue = access.issue(issueId);
        access.requireMember(access.project(issue.getProjectId()), userId);
        return mapper.toResponse(issue);
    }

    @Override
    public IssueResponse update(UUID issueId, IssueUpdateRequest r, UUID userId) {
        Issue issue = access.issue(issueId);
        Project project = access.project(issue.getProjectId());
        access.require(project, userId, Role.PROJECT_MANAGER, Role.TEAM_LEAD);
        if (issue.getStatus() == IssueStatus.CLOSED) {
            throw new BadRequestException("Closed issues cannot be edited");
        }

        if (r.getTitle() != null) {
            String title = r.getTitle().trim();
            if (title.isEmpty()) {
                throw new BadRequestException("Title cannot be blank");
            }
            track(issueId, userId, "title", issue.getTitle(), title);
            issue.setTitle(title);
        }
        if (r.getDescription() != null) {
            track(issueId, userId, "description", issue.getDescription(), r.getDescription());
            issue.setDescription(r.getDescription());
        }
        if (r.getPriority() != null) {
            track(issueId, userId, "priority", issue.getPriority(), r.getPriority());
            issue.setPriority(r.getPriority());
        }
        if (r.getDueDate() != null) {
            track(issueId, userId, "dueDate", issue.getDueDate(), r.getDueDate());
            issue.setDueDate(r.getDueDate());
        }
        if (r.getEstimatedHours() != null) {
            track(issueId, userId, "estimatedHours", issue.getEstimatedHours(), r.getEstimatedHours());
            issue.setEstimatedHours(r.getEstimatedHours());
        }

        issueRepository.save(issue);
        return mapper.toResponse(issue);
    }

    @Override
    public void delete(UUID issueId, UUID userId) {
        Issue issue = access.issue(issueId);
        Project project = access.project(issue.getProjectId());
        Role role = access.requireMember(project, userId);

        boolean allowed = role == Role.ADMIN
                || role == Role.PROJECT_MANAGER
                || role == Role.TEAM_LEAD
                || (role == Role.REPORTER && userId.equals(issue.getCreatedBy()));
        if (!allowed) {
            throw new ForbiddenException("You don't have permission to delete this issue");
        }
        purge(issue);
    }

    @Override
    public IssueResponse resolve(UUID issueId, UUID userId) {
        Issue issue = access.issue(issueId);
        Project project = access.project(issue.getProjectId());
        Role role = access.requireMember(project, userId);

        boolean assignee = assigneeRepository.existsByIssueIdAndUserId(issueId, userId);
        boolean allowed = role == Role.ADMIN
                || role == Role.PROJECT_MANAGER
                || role == Role.TEAM_LEAD
                || (role == Role.CONTRIBUTOR && assignee);
        if (!allowed) {
            throw new ForbiddenException(
                    "Only the team lead, project manager or an assigned contributor can resolve this issue");
        }
        if (issue.getStatus() == IssueStatus.RESOLVED || issue.getStatus() == IssueStatus.CLOSED) {
            throw new BadRequestException("This issue is already resolved or closed");
        }

        IssueStatus old = issue.getStatus();
        issue.setStatus(IssueStatus.RESOLVED);
        issue.setResolvedAt(Instant.now());
        issueRepository.save(issue);

        history.record(issueId, userId, IssueHistoryAction.RESOLVED, "status", old, issue.getStatus());
        return mapper.toResponse(issue);
    }

    @Override
    public IssueResponse approveReview(UUID issueId, ReviewRequest request, UUID userId) {
        return review(issueId, request, userId, true);
    }

    @Override
    public IssueResponse rejectReview(UUID issueId, ReviewRequest request, UUID userId) {
        return review(issueId, request, userId, false);
    }

    private IssueResponse review(UUID issueId, ReviewRequest request, UUID userId, boolean approve) {
        Issue issue = access.issue(issueId);
        Project project = access.project(issue.getProjectId());
        access.require(project, userId, Role.PROJECT_MANAGER, Role.TEAM_LEAD);
        if (issue.getStatus() != IssueStatus.RESOLVED) {
            throw new BadRequestException("Only resolved issues can be reviewed");
        }

        IssueStatus old = issue.getStatus();
        if (approve) {
            issue.setStatus(IssueStatus.CLOSED);
            issue.setClosedAt(Instant.now());
        } else {
            issue.setStatus(IssueStatus.REOPENED);
            issue.setResolvedAt(null);
        }
        issueRepository.save(issue);

        history.record(issueId, userId,
                approve ? IssueHistoryAction.REVIEW_APPROVED : IssueHistoryAction.REVIEW_REJECTED,
                "status", old, issue.getStatus(),
                request == null ? null : request.getComment());
        return mapper.toResponse(issue);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IssueHistoryResponse> history(UUID issueId, UUID userId) {
        Issue issue = access.issue(issueId);
        access.requireMember(access.project(issue.getProjectId()), userId);
        return historyRepository.findByIssueIdOrderByCreatedAtDesc(issueId).stream()
                .map(h -> IssueHistoryResponse.builder()
                        .id(h.getId())
                        .actorId(h.getActorId())
                        .action(h.getAction())
                        .field(h.getFieldName())
                        .oldValue(h.getOldValue())
                        .newValue(h.getNewValue())
                        .comment(h.getComment())
                        .createdAt(h.getCreatedAt())
                        .build())
                .toList();
    }

    @Override
    public LabelResponse addLabel(UUID issueId, IssueLabelRequest request, UUID userId) {
        Issue issue = access.issue(issueId);
        Project project = access.project(issue.getProjectId());
        access.require(project, userId, Role.PROJECT_MANAGER, Role.TEAM_LEAD);

        Label label = labelRepository.findById(request.getLabelId())
                .orElseThrow(() -> new ResourceNotFoundException("Label not found"));
        if (!label.getProjectId().equals(issue.getProjectId())) {
            throw new BadRequestException("Label does not belong to this project");
        }
        if (issueLabelRepository.existsByIssueIdAndLabelId(issueId, label.getId())) {
            throw new BadRequestException("Label is already on this issue");
        }

        IssueLabel link = new IssueLabel();
        link.setIssueId(issueId);
        link.setLabelId(label.getId());
        issueLabelRepository.save(link);

        history.record(issueId, userId, IssueHistoryAction.LABEL_ADDED, "label", null, label.getName());
        return toLabelResponse(label);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabelResponse> labels(UUID issueId, UUID userId) {
        Issue issue = access.issue(issueId);
        access.requireMember(access.project(issue.getProjectId()), userId);
        List<UUID> labelIds = issueLabelRepository.findByIssueId(issueId).stream()
                .map(IssueLabel::getLabelId)
                .toList();
        return labelRepository.findAllById(labelIds).stream()
                .map(this::toLabelResponse)
                .toList();
    }

    @Override
    public void deleteAllForProject(UUID projectId) {
        issueRepository.findByProjectId(projectId).forEach(this::purge);
    }

    private void purge(Issue issue) {
        UUID id = issue.getId();
        attachmentService.deleteAllForIssue(id);
        issueLabelRepository.deleteByIssueId(id);
        assigneeRepository.deleteByIssueId(id);
        requestRepository.deleteByIssueId(id);
        historyRepository.deleteByIssueId(id);
        issueRepository.delete(issue);
    }

    private void track(UUID issueId, UUID userId, String field, Object oldValue, Object newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            history.record(issueId, userId, IssueHistoryAction.UPDATED, field, oldValue, newValue);
        }
    }

    private LabelResponse toLabelResponse(Label label) {
        LabelResponse response = new LabelResponse();
        response.setId(label.getId());
        response.setName(label.getName());
        response.setColor(label.getColor());
        return response;
    }
}