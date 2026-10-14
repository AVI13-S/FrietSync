package com.frietsync.backend.service.impl.issue;

import java.util.UUID;

public record S3CleanupQueuedEvent(UUID cleanupId) {
}
