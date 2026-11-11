package com.frietsync.backend.service.impl.chat;

import com.frietsync.backend.entity.chat.ChatMessage;
import com.frietsync.backend.repository.chat.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatMessageEmbeddingService {

    private static final Logger LOG = LoggerFactory.getLogger(ChatMessageEmbeddingService.class);

    private final EmbeddingModel embeddingModel;
    private final ChatMessageRepository chatMessageRepository;

    @Transactional
    public void generateAndSaveIfMissing(ChatMessage message) {
        String vector;
        try {
            vector = formatVector(embeddingModel.embed(message.getContent()));
        } catch (RuntimeException ex) {
            LOG.warn("Embedding chat message {} failed: {}", message.getId(), ex.getMessage());
            return;
        }
        chatMessageRepository.updateEmbeddingIfMissing(message.getId(), vector);
    }

    private String formatVector(float[] vector) {
        StringBuilder result = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                result.append(",");
            }
            result.append(vector[i]);
        }
        return result.append("]").toString();
    }
}