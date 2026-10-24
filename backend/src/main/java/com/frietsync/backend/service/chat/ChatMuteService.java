package com.frietsync.backend.service.chat;

import com.frietsync.backend.dto.chat.ChatMuteResponse;

import java.time.Instant;
import java.util.UUID;

public interface ChatMuteService {

    ChatMuteResponse mute(UUID projectId, UUID userId, Instant mutedUntil);

    ChatMuteResponse unmute(UUID projectId, UUID userId);

    ChatMuteResponse status(UUID projectId, UUID userId);
}