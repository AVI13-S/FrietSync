package com.frietsync.backend.service.notification;

import com.frietsync.backend.entity.notification.NotificationType;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.UUID;

public interface NotificationPublisher {

    void publish(UUID recipientId, NotificationType type, String title, String message,
                 UUID projectId, UUID issueId, UUID actorId);

    default void publishAll(Collection<UUID> recipients, NotificationType type, String title, String message,
                            UUID projectId, UUID issueId, UUID actorId) {
        if (recipients == null) {
            return;
        }
        new LinkedHashSet<>(recipients)
                .forEach(r -> publish(r, type, title, message, projectId, issueId, actorId));
    }
}