package com.frietsync.backend.dto.issue;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AssignmentRequestBody {

    @Size(max = 500, message = "Message must be at most 500 characters")
    private String message;
}
