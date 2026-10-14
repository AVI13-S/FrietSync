package com.frietsync.backend.repository.issue;

import com.frietsync.backend.entity.issue.S3ObjectCleanup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface S3ObjectCleanupRepository extends JpaRepository<S3ObjectCleanup, UUID> {

    Optional<S3ObjectCleanup> findByBucketNameAndObjectKey(String bucketName, String objectKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from S3ObjectCleanup item where item.id = :id")
    Optional<S3ObjectCleanup> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<S3ObjectCleanup> findTop50ByNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(Instant time);
}
