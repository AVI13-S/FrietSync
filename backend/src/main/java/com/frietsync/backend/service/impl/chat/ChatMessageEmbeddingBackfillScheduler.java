package com.frietsync.backend.service.impl.chat;

import com.frietsync.backend.entity.chat.ChatMessage;
import com.frietsync.backend.repository.chat.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ChatMessageEmbeddingBackfillScheduler {

    private static final int BATCH_SIZE = 20;

    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageEmbeddingService chatMessageEmbeddingService;

    @Scheduled(
            fixedDelayString = "${app.ai.chat-embedding-backfill-delay-ms:60000}",
            initialDelayString = "${app.ai.chat-embedding-backfill-initial-delay-ms:60000}")
    public void backfillMissingEmbeddings() {
        List<ChatMessage> messages = chatMessageRepository.findMessagesWithoutEmbedding(BATCH_SIZE);
        for (ChatMessage message : messages) {
            chatMessageEmbeddingService.generateAndSaveIfMissing(message);
        }
    }
}