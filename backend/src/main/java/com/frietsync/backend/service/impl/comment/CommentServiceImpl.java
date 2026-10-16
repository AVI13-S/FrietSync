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
import com.frietsync.backend.repository.issue.IssueCommentRepository;
import com.frietsync.backend.repository.sprint.SprintCommentRepository;
import com.frietsync.backend.repository.sprint.SprintRepository;
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

    @Override
    public IssueCommentResponse addIssueComment(UUID issueId, CreateIssueCommentRequest request, UUID userId) {
        Issue issue = projectAccess.issue(issueId);
        projectAccess.requireMember(projectAccess.project(issue.getProjectId()), userId);
        IssueComment comment = new IssueComment();
        comment.setIssueId(issueId);
        comment.setUserId(userId);
        comment.setContent(request.getContent().trim());
        return IssueCommentResponse.fromEntity(issueCommentRepository.save(comment));
    }

    @Override
    @Transactional(readOnly = true)
    public List<IssueCommentResponse> listIssueComments(UUID issueId, UUID userId) {
        Issue issue = projectAccess.issue(issueId);
        projectAccess.requireMember(projectAccess.project(issue.getProjectId()), userId);
        return issueCommentRepository.findByIssueIdOrderByCreatedAtAsc(issueId).stream()
                .map(IssueCommentResponse::fromEntity).toList();
    }

    @Override
    public SprintCommentResponse addSprintComment(UUID sprintId, CreateSprintCommentRequest request, UUID userId) {
        Sprint sprint = findSprint(sprintId);
        projectAccess.requireMember(projectAccess.project(sprint.getProjectId()), userId);
        SprintComment comment = new SprintComment();
        comment.setSprintId(sprintId);
        comment.setUserId(userId);
        comment.setContent(request.getContent().trim());
        return SprintCommentResponse.fromEntity(sprintCommentRepository.save(comment));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SprintCommentResponse> listSprintComments(UUID sprintId, UUID userId) {
        Sprint sprint = findSprint(sprintId);
        projectAccess.requireMember(projectAccess.project(sprint.getProjectId()), userId);
        return sprintCommentRepository.findBySprintIdOrderByCreatedAtAsc(sprintId).stream()
                .map(SprintCommentResponse::fromEntity).toList();
    }

    private Sprint findSprint(UUID sprintId) {
        return sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint not found"));
    }
}
