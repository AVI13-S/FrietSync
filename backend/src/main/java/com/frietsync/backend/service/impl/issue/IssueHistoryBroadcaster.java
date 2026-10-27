package com.frietsync.backend.service.impl.issue;

import com.frietsync.backend.dto.issue.IssueHistoryResponse;
import com.frietsync.backend.entity.issue.IssueHistory;
import com.frietsync.backend.entity.user.User;
import com.frietsync.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class IssueHistoryBroadcaster {

    public static final String TOPIC_PREFIX = "/topic/issues/";
    public static final String TOPIC_SUFFIX = "/history";

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onRecorded(IssueHistoryRecordedEvent event) {
        IssueHistory h = event.history();
        try {
            String actorName = h.getActorId() == null ? null
                    : userRepository.findById(h.getActorId()).map(User::getName).orElse(null);
            IssueHistoryResponse payload = IssueHistoryResponse.builder()
                    .id(h.getId())
                    .actorId(h.getActorId())
                    .actorName(actorName)
                    .action(h.getAction())
                    .field(h.getFieldName())
                    .oldValue(h.getOldValue())
                    .newValue(h.getNewValue())
                    .comment(h.getComment())
                    .createdAt(h.getCreatedAt())
                    .build();
            messagingTemplate.convertAndSend(TOPIC_PREFIX + h.getIssueId() + TOPIC_SUFFIX, payload);
        } catch (RuntimeException e) {

            log.warn("Could not broadcast history for issue {}", h.getIssueId(), e);
        }
    }
}