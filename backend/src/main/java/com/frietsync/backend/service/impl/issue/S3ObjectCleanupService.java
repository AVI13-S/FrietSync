package com.frietsync.backend.service.impl.issue;

import com.frietsync.backend.entity.issue.S3ObjectCleanup;
import com.frietsync.backend.repository.issue.S3ObjectCleanupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3ObjectCleanupService {

    private final S3ObjectCleanupRepository cleanupRepository;
    private final S3Client s3Client;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void enqueue(String bucketName, String objectKey) {
        S3ObjectCleanup item = cleanupRepository.findByBucketNameAndObjectKey(bucketName, objectKey)
                .orElseGet(S3ObjectCleanup::new);
        item.setBucketName(bucketName);
        item.setObjectKey(objectKey);
        if (item.getNextAttemptAt() == null) {
            item.setNextAttemptAt(Instant.now());
        }
        S3ObjectCleanup saved = cleanupRepository.save(item);
        eventPublisher.publishEvent(new S3CleanupQueuedEvent(saved.getId()));
    }

    @Async
    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void attempt(S3CleanupQueuedEvent event) {
        cleanupRepository.findByIdForUpdate(event.cleanupId()).ifPresent(this::attemptDelete);
    }

    private void attemptDelete(S3ObjectCleanup item) {
        try {
            delete(item.getBucketName(), item.getObjectKey());
            cleanupRepository.delete(item);
        } catch (RuntimeException exception) {
            recordFailure(item, exception);
        }
    }

    @Scheduled(fixedDelayString = "${app.s3.cleanup.delay-ms:30000}")
    @Transactional
    public void retryPendingDeletes() {
        List<S3ObjectCleanup> due = cleanupRepository
                .findTop50ByNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(Instant.now());
        for (S3ObjectCleanup item : due) {
            attemptDelete(item);
        }
    }

    private void recordFailure(S3ObjectCleanup item, RuntimeException exception) {
        item.setAttempts(item.getAttempts() + 1);
        item.setNextAttemptAt(Instant.now().plusSeconds(backoffSeconds(item.getAttempts())));
        String message = exception.getMessage();
        item.setLastError(message == null ? exception.getClass().getSimpleName()
                : message.substring(0, Math.min(message.length(), 1000)));
        log.error("S3 deletion attempt {} failed for {} in bucket {}",
                item.getAttempts(), item.getObjectKey(), item.getBucketName(), exception);
    }

    private void delete(String bucketName, String objectKey) {
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(objectKey).build());
    }

    private long backoffSeconds(int attempts) {
        return Math.min(21_600L, 30L * (1L << Math.min(attempts, 10)));
    }
}
