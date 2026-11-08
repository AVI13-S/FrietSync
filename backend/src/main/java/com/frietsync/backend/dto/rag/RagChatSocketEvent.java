package com.frietsync.backend.dto.rag;

import java.util.UUID;

public record RagChatSocketEvent(
        String type,
        UUID requestId,
        RagChatResponse response,
        String error
) {
}
