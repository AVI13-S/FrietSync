package com.frietsync.backend.invite.service.impl;

import com.frietsync.backend.common.exception.BadRequestException;
import com.frietsync.backend.common.mail.EmailService;
import com.frietsync.backend.invite.dto.InviteRequest;
import com.frietsync.backend.invite.dto.InviteResponse;
import com.frietsync.backend.invite.entity.Invite;
import com.frietsync.backend.invite.enums.InviteStatus;
import com.frietsync.backend.invite.repository.InviteRepository;
import com.frietsync.backend.invite.service.InviteService;
import com.frietsync.backend.user.entity.User;
import com.frietsync.backend.user.enums.Role;
import com.frietsync.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class InviteServiceImpl implements InviteService {

    private final InviteRepository inviteRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public InviteServiceImpl(InviteRepository inviteRepository,
                             UserRepository userRepository,
                             EmailService emailService) {
        this.inviteRepository = inviteRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public InviteResponse createInvite(InviteRequest request, UUID adminId) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new BadRequestException("Email is required");
        }
        String email = request.getEmail().trim().toLowerCase();

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new BadRequestException("Admin not found"));

        Invite invite = new Invite();
        invite.setEmail(email);
        invite.setRole(Role.CONTRIBUTOR);
        invite.setStatus(InviteStatus.PENDING);
        invite.setInvitedBy(admin.getId());
        inviteRepository.save(invite);

        emailService.sendInviteMail(email, invite.getRole().name());

        return toResponse(invite);
    }

    @Override
    public List<InviteResponse> myPendingInvites(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        List<Invite> invites = inviteRepository
                .findByEmailAndStatusOrderByCreatedAtDesc(
                        user.getEmail(),
                        InviteStatus.PENDING
                );

        List<InviteResponse> responses = new ArrayList<>();

        for (Invite invite : invites) {
            responses.add(toResponse(invite));
        }

        return responses;
    }

    private InviteResponse toResponse(Invite i) {
        InviteResponse r = new InviteResponse();
        r.setId(i.getId());
        r.setEmail(i.getEmail());
        r.setRole(i.getRole());
        r.setStatus(i.getStatus());
        return r;
    }

    @Override
    @Transactional
    public InviteResponse acceptInvite(UUID userId, UUID inviteId) {
        if (inviteId == null) {
            throw new BadRequestException("inviteId is required");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        Invite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new BadRequestException("Invite not found"));

        if (!invite.getEmail().equalsIgnoreCase(user.getEmail())) {
            throw new BadRequestException("This invite does not belong to your account");
        }
        if (invite.getStatus() != InviteStatus.PENDING) {
            throw new BadRequestException("This invite is no longer pending");
        }

        user.setRole(invite.getRole());
        userRepository.save(user);

        invite.setStatus(InviteStatus.ACCEPTED);
        inviteRepository.save(invite);

        return toResponse(invite);
    }
}