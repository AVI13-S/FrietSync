package com.frietsync.backend.dto.project;

import com.frietsync.backend.entity.project.Milestone;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MilestoneResponse(
        UUID id,
        UUID projectId,
        String name,
        String description,
        LocalDate dueDate,
        double progressPercentage,
        Instant createdAt,
        Instant updatedAt
) {

    public static MilestoneResponse fromEntity(Milestone milestone, double progressPercentage) {
        return new MilestoneResponse(
                milestone.getId(),
                milestone.getProjectId(),
                milestone.getName(),
                milestone.getDescription(),
                milestone.getDueDate(),
                progressPercentage,
                milestone.getCreatedAt(),
                milestone.getUpdatedAt()
        );
    }
}
