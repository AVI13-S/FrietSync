package com.frietsync.backend.dto.rag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RagChatRequest {

    @NotBlank
    @Size(max = 1000)
    private String question;
}