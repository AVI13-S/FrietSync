package com.frietsync.backend.controller.notification;

import com.frietsync.backend.dto.notification.*;
import com.frietsync.backend.service.notification.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<NotificationPageResponse> list(
            @RequestParam(defaultValue = "false") boolean unread,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(notificationService.list(userId, unread, page, size));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markRead(@PathVariable UUID id,
                                                         @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(notificationService.markRead(id, userId));
    }

    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllRead(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(Map.of("updated", notificationService.markAllRead(userId)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(Map.of("unreadCount", notificationService.unreadCount(userId)));
    }

    @GetMapping("/preferences")
    public ResponseEntity<List<NotificationPreferenceResponse>> preferences(
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(notificationService.preferences(userId));
    }

    @PutMapping("/preferences")
    public ResponseEntity<List<NotificationPreferenceResponse>> updatePreferences(
            @Valid @RequestBody UpdatePreferenceRequest request,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(notificationService.updatePreferences(userId, request));
    }
}