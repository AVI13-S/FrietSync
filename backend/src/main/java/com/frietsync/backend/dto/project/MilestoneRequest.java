package com.frietsync.backend.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MilestoneRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 200)
    private String description;

    @NotNull
    private LocalDate dueDate;
}
