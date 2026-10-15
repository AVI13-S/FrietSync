package com.frietsync.backend.dto.user;

import com.frietsync.backend.entity.user.Role;

import java.util.UUID;

public record ProfileResponse(
        UUID id,
        UUID workspaceId,
        String name,
        String email,
        String bio,
        String jobTitle,
        Role role,
        String githubUrl,
        String linkedinUrl,
        String portfolioUrl,
        String profilePictureUrl
) {
}
