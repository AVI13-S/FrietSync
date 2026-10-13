package com.frietsync.backend.service.impl.notification;

import com.frietsync.backend.dto.notification.NotificationResponse;
import com.frietsync.backend.entity.notification.Notification;
import com.frietsync.backend.entity.notification.NotificationPreference;
import com.frietsync.backend.entity.notification.NotificationType;
import com.frietsync.backend.repository.notification.NotificationPreferenceRepository;
import com.frietsync.backend.repository.notification.NotificationRepository;
import com.frietsync.backend.service.notification.NotificationEvent;
import com.frietsync.backend.service.notification.NotificationPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationPublisherImpl implements NotificationPublisher {

    private static final int TITLE_MAX = 200;
    private static final int MESSAGE_MAX = 1000;

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional
    public void publish(UUID recipientId, NotificationType type, String title, String message,
                        UUID projectId, UUID issueId, UUID actorId) {
        if (recipientId == null || recipientId.equals(actorId)) {
            return;
        }

        NotificationPreference pref = preferenceRepository.findByUserIdAndType(recipientId, type).orElse(null);
        boolean inApp = pref == null || pref.isInApp();
        boolean email = pref != null && pref.isEmailEnabled();
        if (!inApp && !email) {
            return;
        }

        NotificationResponse created = null;
        if (inApp) {
            Notification n = new Notification();
            n.setUserId(recipientId);
            n.setType(type);
            n.setTitle(cut(title, TITLE_MAX));
            n.setMessage(cut(message, MESSAGE_MAX));
            n.setProjectId(projectId);
            n.setIssueId(issueId);
            n.setActorId(actorId);
            created = NotificationResponse.fromEntity(notificationRepository.save(n));
        }
        events.publishEvent(new NotificationEvent(recipientId, type, n(title), n(message), created, email));
    }

    private String cut(String value, int max) {
        String s = n(value);
        return s.length() > max ? s.substring(0, max) : s;
    }

    private String n(String value) {
        return value == null ? "" : value;
    }
}