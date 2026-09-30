package com.frietsync.backend.invite.controller;

import com.frietsync.backend.invite.dto.InviteRequest;
import com.frietsync.backend.invite.dto.InviteResponse;
import com.frietsync.backend.invite.service.InviteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class InviteController {

    private final InviteService inviteService;

    public InviteController(InviteService inviteService) {
        this.inviteService = inviteService;
    }

    @PostMapping("/api/admin/invites")
    public ResponseEntity<InviteResponse> create(@Valid @RequestBody InviteRequest request,
                                                 Authentication authentication) {
        UUID adminId = UUID.fromString(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inviteService.createInvite(request, adminId));
    }

    @GetMapping("/api/invites/me")
    public ResponseEntity<List<InviteResponse>> myInvites(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(inviteService.myPendingInvites(userId));
    }

    @PostMapping("/api/invites/accept")
    public ResponseEntity<InviteResponse> accept(@Valid @RequestBody InviteRequest request,
                                                 Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(inviteService.acceptInvite(userId, request.getInviteId()));
    }
}