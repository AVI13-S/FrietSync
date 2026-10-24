package com.frietsync.backend.dto.chat;

import lombok.Data;

import java.time.Instant;

@Data
public class ChatMuteRequest {

    private Instant mutedUntil;
}