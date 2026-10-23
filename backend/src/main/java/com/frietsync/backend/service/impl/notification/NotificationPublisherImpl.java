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
        boolean email = type != NotificationType.MEETING_INVITE && pref != null && pref.isEmailEnabled();
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

    @Override
    @Transactional
    public void publishChatMessage(UUID recipientId, String title, String message, UUID projectId, UUID actorId) {
        if (recipientId == null || recipientId.equals(actorId)) {
            return;
        }

        NotificationType type = NotificationType.CHAT_MESSAGE;
        NotificationPreference pref = preferenceRepository.findByUserIdAndType(recipientId, type).orElse(null);
        if (pref != null && !pref.isInApp()) {
            return;
        }

        notificationRepository.deleteUnreadByUserAndTypeAndProject(recipientId, type, projectId);

        Notification notification = new Notification();
        notification.setUserId(recipientId);
        notification.setType(type);
        notification.setTitle(cut(title, TITLE_MAX));
        notification.setMessage(cut(message, MESSAGE_MAX));
        notification.setProjectId(projectId);
        notification.setActorId(actorId);
        NotificationResponse created = NotificationResponse.fromEntity(notificationRepository.save(notification));
        events.publishEvent(new NotificationEvent(recipientId, type, n(title), n(message), created, false));
    }

    private String cut(String value, int max) {
        String s = n(value);
        return s.length() > max ? s.substring(0, max) : s;
    }

    private String n(String value) {
        return value == null ? "" : value;
    }
}
