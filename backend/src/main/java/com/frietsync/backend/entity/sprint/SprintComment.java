package com.frietsync.backend.entity.sprint;

import com.frietsync.backend.entity.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sprint_comments")
public class SprintComment extends BaseEntity {
    @Column(name = "sprint_id", nullable = false)
    private UUID sprintId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 2000)
    private String content;
}
