package com.frietsync.backend.dto.notification;

import com.frietsync.backend.entity.notification.NotificationType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationPreferenceResponse {

    private NotificationType type;
    private boolean inApp;
    private boolean email;
}