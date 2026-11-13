package com.frietsync.backend.service.rag;

import com.frietsync.backend.dto.rag.RagChatResponse;

import java.util.UUID;
import java.util.function.Consumer;

public interface RagChatService {

    RagChatResponse askRagChatbot(UUID projectId, String userQuestion, UUID userId);

    RagChatResponse askRagChatbotStreaming(UUID projectId, String userQuestion, UUID userId,
                                           Consumer<String> chunkHandler);

    void saveFeedback(UUID messageId, String feedback, UUID userId);
}
