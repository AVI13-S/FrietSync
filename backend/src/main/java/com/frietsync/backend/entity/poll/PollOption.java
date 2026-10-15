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
@Table(name = "poll_options", uniqueConstraints = @UniqueConstraint(
        name = "uk_poll_option_order", columnNames = {"poll_id", "option_order"}))
public class PollOption extends BaseEntity {
    @Column(name = "poll_id", nullable = false)
    private UUID pollId;
    @Column(nullable = false, length = 300)
    private String label;
    @Column(name = "option_order", nullable = false)
    private int optionOrder;
    @Column(name = "vote_count", nullable = false)
    private long voteCount;
}
