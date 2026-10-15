package com.frietsync.backend.entity.poll;

import com.frietsync.backend.entity.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "poll_votes", uniqueConstraints = @UniqueConstraint(
        name = "uk_poll_vote_selection", columnNames = {"poll_id", "user_id", "option_id"}))
public class PollVote extends BaseEntity {
    @Column(name = "poll_id", nullable = false)
    private UUID pollId;
    @Column(name = "option_id", nullable = false)
    private UUID optionId;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
}
