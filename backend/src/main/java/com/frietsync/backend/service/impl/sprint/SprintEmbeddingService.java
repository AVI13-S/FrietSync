package com.frietsync.backend.service.impl.sprint;

import com.frietsync.backend.entity.sprint.Sprint;
import com.frietsync.backend.repository.sprint.SprintRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SprintEmbeddingService {
    private static final Logger LOG = LoggerFactory.getLogger(SprintEmbeddingService.class);
    private final EmbeddingModel embeddingModel;
    private final SprintRepository sprintRepository;

    @Transactional
    public void generateAndSave(Sprint sprint) {
        generate(sprint, false);
    }

    @Transactional
    public void generateAndSaveIfMissing(Sprint sprint) {
        generate(sprint, true);
    }

    private void generate(Sprint sprint, boolean onlyIfMissing) {
        String text = "Sprint: " + sprint.getName() + "\nGoal: "
                + (sprint.getGoal() == null ? "" : sprint.getGoal());
        String vector;
        try {
            vector = formatVector(embeddingModel.embed(text));
        } catch (RuntimeException ex) {
            LOG.warn("Embedding sprint {} failed: {}", sprint.getId(), ex.getMessage());
            if (!onlyIfMissing) {
                sprintRepository.clearEmbedding(sprint.getId());
            }
            return;
        }
        if (onlyIfMissing) {
            sprintRepository.updateEmbeddingIfMissing(sprint.getId(), vector);
        } else {
            sprintRepository.updateEmbedding(sprint.getId(), vector);
        }
    }

    private String formatVector(float[] vector) {
        StringBuilder result = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) result.append(',');
            result.append(vector[i]);
        }
        return result.append(']').toString();
    }
}