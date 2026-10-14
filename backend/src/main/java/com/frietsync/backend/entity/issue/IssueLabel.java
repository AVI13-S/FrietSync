package com.frietsync.backend.entity.issue;

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
@Table(name = "issue_labels",
        uniqueConstraints = @UniqueConstraint(columnNames = {"issue_id", "label_id"}))
public class IssueLabel extends BaseEntity {

    @Column(name = "issue_id", nullable = false)
    private UUID issueId;

    @Column(name = "label_id", nullable = false)
    private UUID labelId;
}
