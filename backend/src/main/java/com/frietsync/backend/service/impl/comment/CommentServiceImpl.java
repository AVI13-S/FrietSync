package com.frietsync.backend.service.impl.comment;

import com.frietsync.backend.dto.issue.CreateIssueCommentRequest;
import com.frietsync.backend.dto.issue.IssueCommentResponse;
import com.frietsync.backend.dto.sprint.CreateSprintCommentRequest;
import com.frietsync.backend.dto.sprint.SprintCommentResponse;
import com.frietsync.backend.entity.issue.Issue;
import com.frietsync.backend.entity.issue.IssueComment;
import com.frietsync.backend.entity.sprint.Sprint;
import com.frietsync.backend.entity.sprint.SprintComment;
import com.frietsync.backend.exception.ResourceNotFoundException;
import com.frietsync.backend.exception.ForbiddenException;
import com.frietsync.backend.repository.issue.IssueCommentRepository;
import com.frietsync.backend.repository.sprint.SprintCommentRepository;
import com.frietsync.backend.repository.sprint.SprintRepository;
import com.frietsync.backend.repository.user.UserRepository;
import com.frietsync.backend.service.comment.CommentService;
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

    @Override
    public IssueCommentResponse addIssueComment(UUID issueId, CreateIssueCommentRequest request, UUID userId) {
        Issue issue = projectAccess.issue(issueId);
        projectAccess.requireMember(projectAccess.project(issue.getProjectId()), userId);
        IssueComment comment = new IssueComment();
        comment.setIssueId(issueId);
        comment.setUserId(userId);
        comment.setContent(request.getContent().trim());
        return toIssueCommentResponse(issueCommentRepository.save(comment));
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
        return toSprintCommentResponse(sprintCommentRepository.save(comment));
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
