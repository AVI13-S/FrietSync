package com.frietsync.backend.repository.chat;

import com.frietsync.backend.entity.chat.ChatMute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatMuteRepository extends JpaRepository<ChatMute, UUID> {

    Optional<ChatMute> findByProjectIdAndUserId(UUID projectId, UUID userId);

    @Query("select m.userId from ChatMute m where m.projectId = :projectId "
            + "and (m.mutedUntil is null or m.mutedUntil > :now)")
    List<UUID> findActiveMutedUserIds(@Param("projectId") UUID projectId, @Param("now") Instant now);

    @Modifying
    @Query("delete from ChatMute m where m.projectId = :projectId and m.userId = :userId")
    void deleteByProjectIdAndUserId(@Param("projectId") UUID projectId, @Param("userId") UUID userId);
}