package com.frietsync.backend.dto.issue;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class IssueLabelRequest {

    @NotNull(message = "labelId is required")
    private UUID labelId;
}