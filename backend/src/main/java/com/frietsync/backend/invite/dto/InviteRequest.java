package com.frietsync.backend.invite.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class InviteRequest {
    private String email;
    private UUID inviteId;
}