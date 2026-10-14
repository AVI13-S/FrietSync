package com.frietsync.backend.dto.sprint;

import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class UpdateSprintRequest {
    private String name;
    private String goal;
    private LocalDate startDate;
    private LocalDate endDate;
    private UUID milestoneId;
}