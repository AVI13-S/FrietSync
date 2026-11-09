package com.frietsync.backend.dto.rag;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class RagChatResponse {

    private UUID messageId;
    private String answer;
    private List<RagSourceResponse> sources = new ArrayList<>();
}