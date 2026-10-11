package com.frietsync.backend.repository.notification;

import com.frietsync.backend.entity.notification.NotificationPreference;
import com.frietsync.backend.entity.notification.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {

    List<NotificationPreference> findByUserId(UUID userId);

    Optional<NotificationPreference> findByUserIdAndType(UUID userId, NotificationType type);
}