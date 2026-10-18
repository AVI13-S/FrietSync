package com.frietsync.backend.dto.notification;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class NotificationPageResponse {

    private List<NotificationResponse> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}