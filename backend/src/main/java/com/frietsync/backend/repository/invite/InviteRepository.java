package com.frietsync.backend.repository.invite;

import com.frietsync.backend.entity.invite.Invite;
import com.frietsync.backend.entity.invite.InviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InviteRepository extends JpaRepository<Invite, UUID> {

    List<Invite> findByEmailAndStatusOrderByCreatedAtDesc(String email, InviteStatus status);
}