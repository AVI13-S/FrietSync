package com.frietsync.backend.entity.call;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "call_participants", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"call_id", "user_id"})
})
@Data
@NoArgsConstructor
public class CallParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "call_id", nullable = false)
    private UUID callId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "last_active_at", nullable = false)
    private Instant lastActiveAt;

    @Column(name = "connected", nullable = false)
    private boolean connected = true;

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        if (joinedAt == null) {
            joinedAt = now;
        }
        if (lastActiveAt == null) {
            lastActiveAt = now;
        }
    }
}
