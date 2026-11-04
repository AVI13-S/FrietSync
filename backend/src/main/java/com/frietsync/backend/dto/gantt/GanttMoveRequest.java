package com.frietsync.backend.dto.gantt;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class GanttMoveRequest {

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;
}