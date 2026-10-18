package com.frietsync.backend.dto.issue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateIssueCommentRequest {
    @NotBlank
    @Size(max = 2000)
    private String content;
}
