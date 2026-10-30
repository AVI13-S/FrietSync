package com.frietsync.backend.repository.notification;

import com.frietsync.backend.entity.notification.Notification;
import com.frietsync.backend.entity.notification.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Notification> findByUserIdAndSeenOrderByCreatedAtDesc(UUID userId, boolean seen, Pageable pageable);

    long countByUserIdAndSeen(UUID userId, boolean seen);

    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);

    @Modifying
    @Query("delete from Notification n where n.userId = :userId and n.type = :type "
            + "and n.projectId = :projectId and n.seen = false")
    int deleteUnreadByUserAndTypeAndProject(@Param("userId") UUID userId,
                                            @Param("type") NotificationType type,
                                            @Param("projectId") UUID projectId);

    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.seen = true, n.readAt = :now "
            + "where n.userId = :userId and n.seen = false")
    int markAllRead(@Param("userId") UUID userId, @Param("now") Instant now);

    @Modifying
    @Query("delete from Notification n where n.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}
