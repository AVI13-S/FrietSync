package com.frietsync.backend.entity.issue;

import com.frietsync.backend.entity.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "issue_history",
        indexes = @Index(name = "idx_issue_history_issue", columnList = "issue_id"))
public class IssueHistory extends BaseEntity {

    @Column(name = "issue_id", nullable = false)
    private UUID issueId;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private IssueHistoryAction action;

    @Column(name = "field_name", length = 50)
    private String fieldName;

    @Column(name = "old_value", length = 1000)
    private String oldValue;

    @Column(name = "new_value", length = 1000)
    private String newValue;

    @Column(length = 1000)
    private String comment;
}