package com.frietsync.backend.dto.notification;

import com.frietsync.backend.entity.notification.Notification;
import com.frietsync.backend.entity.notification.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class NotificationResponse {

    private UUID id;
    private NotificationType type;
    private String title;
    private String message;
    private UUID projectId;
    private UUID issueId;
    private UUID actorId;
    private boolean read;
    private Instant readAt;
    private Instant createdAt;

    public static NotificationResponse fromEntity(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .projectId(n.getProjectId())
                .issueId(n.getIssueId())
                .actorId(n.getActorId())
                .read(n.isSeen())
                .readAt(n.getReadAt())
                .createdAt(n.getCreatedAt())
                .build();
    }
}