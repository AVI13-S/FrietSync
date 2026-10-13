package com.frietsync.backend.service.impl.notification;

import com.frietsync.backend.dto.notification.NotificationPageResponse;
import com.frietsync.backend.dto.notification.NotificationPreferenceResponse;
import com.frietsync.backend.dto.notification.NotificationResponse;
import com.frietsync.backend.dto.notification.PreferenceUpdate;
import com.frietsync.backend.dto.notification.UpdatePreferenceRequest;
import com.frietsync.backend.entity.notification.Notification;
import com.frietsync.backend.entity.notification.NotificationPreference;
import com.frietsync.backend.entity.notification.NotificationType;
import com.frietsync.backend.exception.ResourceNotFoundException;
import com.frietsync.backend.repository.notification.NotificationPreferenceRepository;
import com.frietsync.backend.repository.notification.NotificationRepository;
import com.frietsync.backend.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;

    @Override
    @Transactional(readOnly = true)
    public NotificationPageResponse list(UUID userId, boolean unreadOnly, int page, int size) {
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        Page<Notification> result = unreadOnly
                ? notificationRepository.findByUserIdAndSeenOrderByCreatedAtDesc(userId, false, pageRequest)
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageRequest);
        return NotificationPageResponse.builder()
                .items(result.getContent().stream().map(NotificationResponse::fromEntity).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Override
    public NotificationResponse markRead(UUID notificationId, UUID userId) {

        Notification n = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!n.isSeen()) {
            n.setSeen(true);
            n.setReadAt(Instant.now());
            notificationRepository.save(n);
        }
        return NotificationResponse.fromEntity(n);
    }

    @Override
    public int markAllRead(UUID userId) {
        return notificationRepository.markAllRead(userId, Instant.now());
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndSeen(userId, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationPreferenceResponse> preferences(UUID userId) {
        Map<NotificationType, NotificationPreference> saved = new HashMap<>();
        preferenceRepository.findByUserId(userId).forEach(p -> saved.put(p.getType(), p));
        return Arrays.stream(NotificationType.values())
                .map(type -> {
                    NotificationPreference p = saved.get(type);
                    return NotificationPreferenceResponse.builder()
                            .type(type)
                            .inApp(p == null || p.isInApp())
                            .email(p != null && p.isEmailEnabled())
                            .build();
                })
                .toList();
    }

    @Override
    public List<NotificationPreferenceResponse> updatePreferences(UUID userId, UpdatePreferenceRequest request) {
        Map<NotificationType, NotificationPreference> existing = new HashMap<>();
        preferenceRepository.findByUserId(userId).forEach(p -> existing.put(p.getType(), p));

        for (PreferenceUpdate update : request.getPreferences()) {
            NotificationPreference p = existing.get(update.getType());
            if (p == null) {
                p = new NotificationPreference();
                p.setUserId(userId);
                p.setType(update.getType());
                existing.put(update.getType(), p);
            }
            if (update.getInApp() != null) {
                p.setInApp(update.getInApp());
            }
            if (update.getEmail() != null) {
                p.setEmailEnabled(update.getEmail());
            }
            preferenceRepository.save(p);
        }
        return preferences(userId);
    }
}
