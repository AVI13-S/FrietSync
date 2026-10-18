package com.frietsync.backend.dto.sprint;

import com.frietsync.backend.entity.sprint.SprintComment;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class SprintCommentResponse {
    private UUID id;
    private UUID sprintId;
    private UUID userId;
    private String content;
    private Instant createdAt;

    public static SprintCommentResponse fromEntity(SprintComment comment) {
        return SprintCommentResponse.builder().id(comment.getId()).sprintId(comment.getSprintId())
                .userId(comment.getUserId()).content(comment.getContent()).createdAt(comment.getCreatedAt()).build();
    }
}
