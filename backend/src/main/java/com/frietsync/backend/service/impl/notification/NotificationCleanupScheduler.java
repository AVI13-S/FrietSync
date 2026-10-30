package com.frietsync.backend.service.impl.notification;

import com.frietsync.backend.repository.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupScheduler {

    private final NotificationRepository notificationRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteExpiredNotifications() {
        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
        int deletedCount = notificationRepository.deleteOlderThan(cutoff);
        if (deletedCount > 0) {
            log.info("Cleaned up {} expired notifications older than 7 days", deletedCount);
        }
    }
}
