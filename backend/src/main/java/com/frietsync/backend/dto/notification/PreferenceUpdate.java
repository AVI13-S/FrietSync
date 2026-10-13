package com.frietsync.backend.dto.notification;

import com.frietsync.backend.entity.notification.NotificationType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data
public class PreferenceUpdate {

    @NotNull(message = "type is required")
    private NotificationType type;

    private Boolean inApp;

    private Boolean email;
}
