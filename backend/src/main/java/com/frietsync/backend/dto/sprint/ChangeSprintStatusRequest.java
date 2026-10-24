package com.frietsync.backend.dto.sprint;

import com.frietsync.backend.entity.sprint.SprintStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeSprintStatusRequest(@NotNull SprintStatus status) {
}
