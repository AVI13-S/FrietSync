package com.frietsync.backend.dto.invite;

import com.frietsync.backend.entity.user.Role;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class InviteRequest {
    private String email;
    private UUID inviteId;
    private Instant expiresAt;
    private Role role;
    private UUID projectId;
}