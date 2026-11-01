package com.frietsync.backend.dto.canvas;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RenameCanvasRequest {

    @NotBlank(message = "Canvas name is required")
    @Size(max = 200, message = "Canvas name must be at most 200 characters")
    private String name;
}