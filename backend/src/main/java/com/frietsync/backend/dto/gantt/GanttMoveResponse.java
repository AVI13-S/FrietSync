package com.frietsync.backend.dto.gantt;

import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class GanttMoveResponse {

    private UUID id;
    private String type;
    private LocalDate startDate;
    private LocalDate endDate;
}