package com.frietsync.backend.invite.repository;

import com.frietsync.backend.invite.entity.Invite;
import com.frietsync.backend.invite.enums.InviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InviteRepository extends JpaRepository<Invite, UUID> {

    List<Invite> findByEmailAndStatusOrderByCreatedAtDesc(String email, InviteStatus status);
}