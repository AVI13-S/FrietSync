package com.frietsync.backend.repository.integration;

import com.frietsync.backend.entity.integration.GitHubConnection;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface GitHubConnectionRepository extends JpaRepository<GitHubConnection, UUID> {
    Optional<GitHubConnection> findByUser_Id(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from GitHubConnection c where c.user.id = :userId")
    Optional<GitHubConnection> findByUserIdForUpdate(@Param("userId") UUID userId);
}
