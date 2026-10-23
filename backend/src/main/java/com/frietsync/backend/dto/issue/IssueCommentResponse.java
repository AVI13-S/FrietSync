package com.frietsync.backend.dto.issue;

import com.frietsync.backend.entity.issue.IssueComment;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class IssueCommentResponse {
    private UUID id;
    private UUID issueId;
    private UUID userId;
    private String userName;
    private String content;
    private Instant createdAt;

    public static IssueCommentResponse fromEntity(IssueComment comment, String userName) {
        return IssueCommentResponse.builder().id(comment.getId()).issueId(comment.getIssueId())
                .userId(comment.getUserId()).userName(userName)
                .content(comment.getContent()).createdAt(comment.getCreatedAt()).build();
    }
}
