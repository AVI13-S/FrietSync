package com.frietsync.backend.dto.chat;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class EditChatMessageRequest {

    @Size(max = 2000)
    private String content;

    @Size(max = 30)
    private List<UUID> mentionedUserIds = new ArrayList<>();
}