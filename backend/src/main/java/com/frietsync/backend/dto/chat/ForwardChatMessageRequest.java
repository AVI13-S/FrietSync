package com.frietsync.backend.dto.chat;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ForwardChatMessageRequest {

    @NotNull
    private UUID targetProjectId;
}