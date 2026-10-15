package com.frietsync.backend.entity.issue;

import com.frietsync.backend.entity.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "s3_object_cleanup",
        indexes = @Index(name = "idx_s3_cleanup_due", columnList = "next_attempt_at"),
        uniqueConstraints = @UniqueConstraint(name = "uk_s3_cleanup_bucket_key", columnNames = {"bucket_name", "object_key"}))
public class S3ObjectCleanup extends BaseEntity {

    @Column(name = "bucket_name", nullable = false, length = 255)
    private String bucketName;

    @Column(name = "object_key", nullable = false, length = 1024)
    private String objectKey;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;
}
