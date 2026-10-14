package com.frietsync.backend.dto.replay;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReplayPhaseUpdateRequest {

    @Size(max = 100, message = "Title must be at most 100 characters")
    private String title;

    @Size(max = 500, message = "Description must be at most 500 characters")
    private String description;
}