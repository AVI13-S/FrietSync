package com.frietsync.backend.service.rag;

import com.frietsync.backend.dto.rag.RagChatResponse;

import java.util.UUID;

public interface RagChatService {

    RagChatResponse askRagChatbot(UUID projectId, String userQuestion, UUID userId);
}