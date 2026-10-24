package com.frietsync.backend.service.impl.comment;

import com.frietsync.backend.dto.issue.CreateIssueCommentRequest;
import com.frietsync.backend.dto.issue.IssueCommentResponse;
import com.frietsync.backend.dto.sprint.CreateSprintCommentRequest;
import com.frietsync.backend.dto.sprint.SprintCommentResponse;
import com.frietsync.backend.entity.issue.Issue;
import com.frietsync.backend.entity.issue.IssueComment;
import com.frietsync.backend.entity.issue.IssueAssignee;
import com.frietsync.backend.entity.notification.NotificationType;
import com.frietsync.backend.entity.project.Project;
import com.frietsync.backend.entity.sprint.Sprint;
import com.frietsync.backend.entity.sprint.SprintComment;
import com.frietsync.backend.exception.ResourceNotFoundException;
import com.frietsync.backend.exception.ForbiddenException;
import com.frietsync.backend.repository.issue.IssueCommentRepository;
import com.frietsync.backend.repository.issue.IssueAssigneeRepository;
import com.frietsync.backend.repository.project.ProjectMemberRepository;
import com.frietsync.backend.repository.sprint.SprintCommentRepository;
import com.frietsync.backend.repository.sprint.SprintRepository;
import com.frietsync.backend.repository.user.UserRepository;
import com.frietsync.backend.service.comment.CommentService;
import com.frietsync.backend.service.notification.NotificationPublisher;
import com.frietsync.backend.service.impl.project.ProjectAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {

    private final IssueCommentRepository issueCommentRepository;
    private final SprintCommentRepository sprintCommentRepository;
    private final SprintRepository sprintRepository;
    private final ProjectAccess projectAccess;
    private final UserRepository userRepository;
    private final IssueAssigneeRepository issueAssigneeRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final NotificationPublisher notificationPublisher;

    @Override
    public IssueCommentResponse addIssueComment(UUID issueId, CreateIssueCommentRequest request, UUID userId) {
        Issue issue = projectAccess.issue(issueId);
        projectAccess.requireMember(projectAccess.project(issue.getProjectId()), userId);
        IssueComment comment = new IssueComment();
        comment.setIssueId(issueId);
        comment.setUserId(userId);
        comment.setContent(request.getContent().trim());
        IssueComment saved = issueCommentRepository.save(comment);
        Project project = projectAccess.project(issue.getProjectId());
        List<UUID> mentioned = validMentions(request.getMentionedUserIds(), project, userId);
        java.util.Set<UUID> recipients = new java.util.LinkedHashSet<>();
        recipients.add(issue.getCreatedBy());
        issueAssigneeRepository.findByIssueId(issueId).stream().map(IssueAssignee::getUserId)
                .forEach(recipients::add);
        addProjectLeads(recipients, project);
        recipients.removeAll(mentioned);
        recipients.forEach(recipient -> notificationPublisher.publish(recipient, NotificationType.COMMENT_ADDED,
                "New issue comment", "A new comment was added to issue #" + issue.getIssueNumber()
                        + ": " + issue.getTitle(), project.getId(), issueId, userId));
        mentioned.forEach(recipient -> notificationPublisher.publish(recipient, NotificationType.MENTION,
                "You were mentioned", "You were mentioned in a comment on issue #" + issue.getIssueNumber()
                        + ": " + issue.getTitle(), project.getId(), issueId, userId));
        return toIssueCommentResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IssueCommentResponse> listIssueComments(UUID issueId, UUID userId) {
        Issue issue = projectAccess.issue(issueId);
        projectAccess.requireMember(projectAccess.project(issue.getProjectId()), userId);
        return issueCommentRepository.findByIssueIdOrderByCreatedAtAsc(issueId).stream()
                .map(this::toIssueCommentResponse).toList();
    }

    @Override
    public IssueCommentResponse updateIssueComment(UUID issueId, UUID commentId,
                                                   CreateIssueCommentRequest request, UUID userId) {
        IssueComment comment = issueCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue comment not found"));
        if (!comment.getIssueId().equals(issueId)) {
            throw new ResourceNotFoundException("Issue comment not found");
        }
        Issue issue = projectAccess.issue(issueId);
        projectAccess.requireMember(projectAccess.project(issue.getProjectId()), userId);
        requireCommentOwner(comment.getUserId(), userId);
        comment.setContent(request.getContent().trim());
        return toIssueCommentResponse(issueCommentRepository.save(comment));
    }

    @Override
    public void deleteIssueComment(UUID issueId, UUID commentId, UUID userId) {
        IssueComment comment = issueCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue comment not found"));
        if (!comment.getIssueId().equals(issueId)) {
            throw new ResourceNotFoundException("Issue comment not found");
        }
        Issue issue = projectAccess.issue(issueId);
        projectAccess.requireMember(projectAccess.project(issue.getProjectId()), userId);
        requireCommentOwner(comment.getUserId(), userId);
        issueCommentRepository.delete(comment);
    }

    @Override
    public SprintCommentResponse addSprintComment(UUID sprintId, CreateSprintCommentRequest request, UUID userId) {
        Sprint sprint = findSprint(sprintId);
        projectAccess.requireMember(projectAccess.project(sprint.getProjectId()), userId);
        SprintComment comment = new SprintComment();
        comment.setSprintId(sprintId);
        comment.setUserId(userId);
        comment.setContent(request.getContent().trim());
        SprintComment saved = sprintCommentRepository.save(comment);
        Project project = projectAccess.project(sprint.getProjectId());
        List<UUID> mentioned = validMentions(request.getMentionedUserIds(), project, userId);
        java.util.Set<UUID> recipients = new java.util.LinkedHashSet<>();
        if (sprint.getCreatedBy() != null) recipients.add(sprint.getCreatedBy());
        if (sprint.getAssignedContributorId() != null) recipients.add(sprint.getAssignedContributorId());
        addProjectLeads(recipients, project);
        recipients.removeAll(mentioned);
        recipients.forEach(recipient -> notificationPublisher.publish(recipient, NotificationType.COMMENT_ADDED,
                "New sprint comment", "A new comment was added to sprint " + sprint.getName() + ".",
                project.getId(), null, userId));
        mentioned.forEach(recipient -> notificationPublisher.publish(recipient, NotificationType.MENTION,
                "You were mentioned", "You were mentioned in a comment on sprint " + sprint.getName() + ".",
                project.getId(), null, userId));
        return toSprintCommentResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SprintCommentResponse> listSprintComments(UUID sprintId, UUID userId) {
        Sprint sprint = findSprint(sprintId);
        projectAccess.requireMember(projectAccess.project(sprint.getProjectId()), userId);
        return sprintCommentRepository.findBySprintIdOrderByCreatedAtAsc(sprintId).stream()
                .map(this::toSprintCommentResponse).toList();
    }

    @Override
    public SprintCommentResponse updateSprintComment(UUID sprintId, UUID commentId,
                                                     CreateSprintCommentRequest request, UUID userId) {
        SprintComment comment = sprintCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint comment not found"));
        if (!comment.getSprintId().equals(sprintId)) {
            throw new ResourceNotFoundException("Sprint comment not found");
        }
        Sprint sprint = findSprint(sprintId);
        projectAccess.requireMember(projectAccess.project(sprint.getProjectId()), userId);
        requireCommentOwner(comment.getUserId(), userId);
        comment.setContent(request.getContent().trim());
        return toSprintCommentResponse(sprintCommentRepository.save(comment));
    }

    @Override
    public void deleteSprintComment(UUID sprintId, UUID commentId, UUID userId) {
        SprintComment comment = sprintCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint comment not found"));
        if (!comment.getSprintId().equals(sprintId)) {
            throw new ResourceNotFoundException("Sprint comment not found");
        }
        Sprint sprint = findSprint(sprintId);
        projectAccess.requireMember(projectAccess.project(sprint.getProjectId()), userId);
        requireCommentOwner(comment.getUserId(), userId);
        sprintCommentRepository.delete(comment);
    }

    private void requireCommentOwner(UUID ownerId, UUID userId) {
        if (!ownerId.equals(userId)) {
            throw new ForbiddenException("You can only edit or delete your own comments");
        }
    }

    private List<UUID> validMentions(List<UUID> mentionedUserIds, Project project, UUID actorId) {
        if (mentionedUserIds == null || mentionedUserIds.isEmpty()) return List.of();
        return mentionedUserIds.stream().filter(java.util.Objects::nonNull).distinct()
                .filter(id -> !id.equals(actorId))
                .peek(id -> projectAccess.requireMember(project, id)).toList();
    }

    private void addProjectLeads(java.util.Set<UUID> recipients, Project project) {
        if (project.getAdminId() != null) recipients.add(project.getAdminId());
        if (project.getProjectManagerId() != null) recipients.add(project.getProjectManagerId());
        if (project.getTeamLeadId() != null) recipients.add(project.getTeamLeadId());
        projectMemberRepository.findByProjectId(project.getId()).stream()
                .filter(member -> member.getRole() == com.frietsync.backend.entity.user.Role.ADMIN
                        || member.getRole() == com.frietsync.backend.entity.user.Role.PROJECT_MANAGER
                        || member.getRole() == com.frietsync.backend.entity.user.Role.TEAM_LEAD)
                .map(com.frietsync.backend.entity.project.ProjectMember::getUserId).forEach(recipients::add);
    }

    private IssueCommentResponse toIssueCommentResponse(IssueComment comment) {
        String userName = userRepository.findById(comment.getUserId()).map(user -> user.getName()).orElse(null);
        return IssueCommentResponse.fromEntity(comment, userName);
    }

    private SprintCommentResponse toSprintCommentResponse(SprintComment comment) {
        String userName = userRepository.findById(comment.getUserId()).map(user -> user.getName()).orElse(null);
        return SprintCommentResponse.fromEntity(comment, userName);
    }

    private Sprint findSprint(UUID sprintId) {
        return sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint not found"));
    }
}
