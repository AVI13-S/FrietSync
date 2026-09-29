package com.frietsync.backend.invite.service;

import com.frietsync.backend.invite.dto.InviteRequest;
import com.frietsync.backend.invite.dto.InviteResponse;

import java.util.List;
import java.util.UUID;

public interface InviteService {
    InviteResponse createInvite(InviteRequest request, UUID adminId);
    List<InviteResponse> myPendingInvites(UUID userId);
    InviteResponse acceptInvite(UUID userId, UUID inviteId);
}