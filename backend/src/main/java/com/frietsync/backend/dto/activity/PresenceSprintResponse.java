package com.frietsync.backend.dto.activity;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class PresenceSprintResponse {

    private UUID id;
    private Integer sprintNumber;
    private String name;
    private LocalDate endDate;
}