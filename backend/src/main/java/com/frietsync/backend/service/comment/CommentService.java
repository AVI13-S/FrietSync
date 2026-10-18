package com.frietsync.backend.service.comment;

import com.frietsync.backend.dto.issue.CreateIssueCommentRequest;
import com.frietsync.backend.dto.issue.IssueCommentResponse;
import com.frietsync.backend.dto.sprint.CreateSprintCommentRequest;
import com.frietsync.backend.dto.sprint.SprintCommentResponse;

import java.util.List;
import java.util.UUID;

public interface CommentService {
    IssueCommentResponse addIssueComment(UUID issueId, CreateIssueCommentRequest request, UUID userId);

    List<IssueCommentResponse> listIssueComments(UUID issueId, UUID userId);

    IssueCommentResponse updateIssueComment(UUID issueId, UUID commentId,
                                             CreateIssueCommentRequest request, UUID userId);

    void deleteIssueComment(UUID issueId, UUID commentId, UUID userId);

    SprintCommentResponse addSprintComment(UUID sprintId, CreateSprintCommentRequest request, UUID userId);

    List<SprintCommentResponse> listSprintComments(UUID sprintId, UUID userId);

    SprintCommentResponse updateSprintComment(UUID sprintId, UUID commentId,
                                               CreateSprintCommentRequest request, UUID userId);

    void deleteSprintComment(UUID sprintId, UUID commentId, UUID userId);
}
