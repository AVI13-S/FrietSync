package com.frietsync.backend.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class ChatMessageRequest {

    @NotBlank
    @Size(max = 2000)
    private String content;

    private UUID replyToId;
    @Size(max = 30)
    private List<UUID> mentionedUserIds = new ArrayList<>();
}