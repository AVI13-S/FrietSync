package com.frietsync.backend.service.impl.rag;

import com.frietsync.backend.repository.rag.RagChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class RagChatHistoryCleanupScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(RagChatHistoryCleanupScheduler.class);

    private final RagChatMessageRepository historyRepository;

    @Value("${app.ai.chat-history-retention-days:30}")
    private int retentionDays;

    @Scheduled(cron = "${app.ai.chat-history-cleanup-cron:0 15 3 * * *}")
    @Transactional
    public void deleteOldHistory() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(retentionDays));
        int deleted = historyRepository.deleteOlderThan(cutoff);
        if (deleted > 0) {
            LOG.info("Deleted {} AI chat messages older than {} days", deleted, retentionDays);
        }
    }
}