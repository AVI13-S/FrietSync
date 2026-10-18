package com.frietsync.backend.dto.issue;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewRequest {

    @Size(max = 500, message = "Comment must be at most 500 characters")
    private String comment;
}
