package com.frietsync.backend.service.notification;

import com.frietsync.backend.dto.notification.NotificationResponse;
import com.frietsync.backend.entity.notification.NotificationType;

import java.util.UUID;

public record NotificationEvent(UUID userId, NotificationType type, String title, String message,
                                NotificationResponse notification, boolean sendEmail) { }