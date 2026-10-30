package com.frietsync.backend.repository.call;

import com.frietsync.backend.entity.call.CallParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CallParticipantRepository extends JpaRepository<CallParticipant, UUID> {

    Optional<CallParticipant> findByCallIdAndUserId(UUID callId, UUID userId);

    List<CallParticipant> findByCallIdAndConnectedTrue(UUID callId);

    List<CallParticipant> findByCallId(UUID callId);

    @Modifying
    @Transactional
    @Query("update CallParticipant p set p.connected = false where p.userId = :userId and p.callId in (select c.id from CallSession c where c.status = 'ACTIVE')")
    int markDisconnectedForUser(@Param("userId") UUID userId);

    @Modifying
    @Transactional
    @Query("update CallParticipant p set p.lastActiveAt = :now, p.connected = true where p.callId = :callId and p.userId = :userId")
    int touchParticipant(@Param("callId") UUID callId, @Param("userId") UUID userId, @Param("now") Instant now);
}
