package com.frietsync.backend.dto.document;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateDocumentRequest {

    @Size(max = 200, message = "Title must be at most 200 characters")
    private String title;

    @Size(max = 50000, message = "Content must be at most 50000 characters")
    private String content;
}