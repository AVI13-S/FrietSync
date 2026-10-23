package com.frietsync.backend.service.chat;

import com.frietsync.backend.dto.chat.ChatMessageRequest;
import com.frietsync.backend.dto.chat.EditChatMessageRequest;
import com.frietsync.backend.dto.chat.ChatMessageResponse;

import java.util.List;
import java.util.UUID;

public interface ChatService {

    ChatMessageResponse sendMessage(UUID projectId, UUID senderId, ChatMessageRequest request);

    List<ChatMessageResponse> getRecentMessages(UUID projectId, UUID userId, int limit);

    ChatMessageResponse editMessage(UUID projectId, UUID userId, UUID messageId, EditChatMessageRequest request);

    ChatMessageResponse deleteMessage(UUID projectId, UUID userId, UUID messageId);

    void assertProjectAccess(UUID projectId, UUID userId);
}