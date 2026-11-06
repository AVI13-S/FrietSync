package com.frietsync.backend.service.impl.document;

import com.frietsync.backend.entity.document.ProjectDocument;
import com.frietsync.backend.repository.document.ProjectDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DocumentEmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final ProjectDocumentRepository documentRepository;

    @Transactional
    public void generateAndSaveEmbedding(ProjectDocument document) {
        try {
            String textToEmbed = "Title: " + document.getTitle() + "\n\nContent:\n" + document.getContent();
            float[] vectorArray = embeddingModel.embed(textToEmbed);
            document.setEmbedding(formatVector(vectorArray));
            documentRepository.save(document);
        } catch (Exception e) {
            // Log or handle embedding exception gracefully if API is unconfigured/unavailable during offline runs
        }
    }

    private String formatVector(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
