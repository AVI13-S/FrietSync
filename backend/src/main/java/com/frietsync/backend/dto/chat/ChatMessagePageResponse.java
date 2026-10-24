package com.frietsync.backend.dto.chat;

import java.util.List;

public record ChatMessagePageResponse(
        List<ChatMessageResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
}
