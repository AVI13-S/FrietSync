package com.frietsync.backend.invite.dto;

import com.frietsync.backend.user.enums.Role;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class InviteRequest {
    private String email;
    private UUID inviteId;
    private Instant expiresAt;
    private Role role;
}