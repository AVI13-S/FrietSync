package com.frietsync.backend.invite.dto;

import com.frietsync.backend.invite.enums.InviteStatus;
import com.frietsync.backend.user.enums.Role;
import lombok.Data;

import java.util.UUID;

@Data
public class InviteResponse {
    private UUID id;
    private String email;
    private Role role;
    private InviteStatus status;
}