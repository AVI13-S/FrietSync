package com.frietsync.backend.dto.chat;

import java.util.UUID;

public record ChatTypingResponse(UUID userId, String userName) {
}