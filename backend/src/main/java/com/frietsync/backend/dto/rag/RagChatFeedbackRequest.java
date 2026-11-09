package com.frietsync.backend.dto.rag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RagChatFeedbackRequest {

    @NotBlank
    @Pattern(regexp = "UP|DOWN", message = "Feedback must be UP or DOWN")
    private String feedback;
}