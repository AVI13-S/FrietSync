package com.frietsync.backend.dto.user;

import java.util.UUID;

public record UserSkillResponse(
        UUID id,
        String skillName
) {
}
