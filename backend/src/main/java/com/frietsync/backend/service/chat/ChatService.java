package com.frietsync.backend.service.chat;

import com.frietsync.backend.dto.chat.ChatMessageRequest;
import com.frietsync.backend.dto.chat.ChatMessageResponse;
import com.frietsync.backend.dto.chat.ChatMessagePageResponse;
import com.frietsync.backend.dto.chat.ChatMessageWindowResponse;

import java.util.List;
import java.util.UUID;

public interface ChatService {

    ChatMessageResponse sendMessage(UUID projectId, UUID senderId, ChatMessageRequest request);

    List<ChatMessageResponse> getRecentMessages(UUID projectId, UUID userId, int page, int size);

    ChatMessagePageResponse searchMessages(UUID projectId, UUID userId, String query, int page, int size);

    ChatMessageWindowResponse getMessagesAround(UUID projectId, UUID userId, UUID messageId,
                                                int before, int after);

    void assertProjectAccess(UUID projectId, UUID userId);
}
