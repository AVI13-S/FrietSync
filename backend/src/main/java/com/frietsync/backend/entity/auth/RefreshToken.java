package com.frietsync.backend.entity.auth;

import com.frietsync.backend.entity.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@Data
public class RefreshToken extends BaseEntity {

    private UUID userId;
    private String token;
    private Instant expiresAt;
    private boolean revoked = false;
}