package com.frietsync.backend.dto.chat;

import java.util.List;
import java.util.UUID;

public record ChatMessageWindowResponse(UUID targetMessageId, List<ChatMessageResponse> items) {
}
