package com.frietsync.backend.entity.thread;

import com.frietsync.backend.entity.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "thread_votes",
        uniqueConstraints = @UniqueConstraint(name = "uk_thread_votes_thread_user", columnNames = {"thread_id", "user_id"}),
        indexes = @Index(name = "idx_thread_votes_user", columnList = "user_id"))
public class ThreadVote extends BaseEntity {

    @Column(name = "thread_id", nullable = false, updatable = false)
    private UUID threadId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;
}