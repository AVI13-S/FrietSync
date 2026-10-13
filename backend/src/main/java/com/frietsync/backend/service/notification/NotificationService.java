package com.frietsync.backend.service.notification;

import com.frietsync.backend.dto.notification.*;

import java.util.List;
import java.util.UUID;

public interface NotificationService {
    NotificationPageResponse list(UUID userId, boolean unreadOnly, int page, int size);

    NotificationResponse markRead(UUID notificationId, UUID userId);

    int markAllRead(UUID userId);

    long unreadCount(UUID userId);

    List<NotificationPreferenceResponse> preferences(UUID userId);

    List<NotificationPreferenceResponse> updatePreferences(UUID userId, UpdatePreferenceRequest request);
}