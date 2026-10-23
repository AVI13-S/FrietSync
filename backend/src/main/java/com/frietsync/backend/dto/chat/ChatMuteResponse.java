package com.frietsync.backend.dto.chat;

import java.time.Instant;

public record ChatMuteResponse(boolean muted, Instant mutedUntil) {
}