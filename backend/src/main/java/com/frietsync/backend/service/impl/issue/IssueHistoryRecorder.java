package com.frietsync.backend.service.impl.issue;

import com.frietsync.backend.entity.issue.IssueHistory;
import com.frietsync.backend.entity.issue.IssueHistoryAction;
import com.frietsync.backend.repository.issue.IssueHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class IssueHistoryRecorder {

    private static final int MAX = 1000;

    private final IssueHistoryRepository historyRepository;

    public void record(UUID issueId, UUID actorId, IssueHistoryAction action) {
        record(issueId, actorId, action, null, null, null, null);
    }

    public void record(UUID issueId, UUID actorId, IssueHistoryAction action,
                       String field, Object oldValue, Object newValue) {
        record(issueId, actorId, action, field, oldValue, newValue, null);
    }

    public void record(UUID issueId, UUID actorId, IssueHistoryAction action,
                       String field, Object oldValue, Object newValue, String comment) {
        IssueHistory entry = new IssueHistory();
        entry.setIssueId(issueId);
        entry.setActorId(actorId);
        entry.setAction(action);
        entry.setFieldName(field);
        entry.setOldValue(text(oldValue));
        entry.setNewValue(text(newValue));
        entry.setComment(text(comment));
        historyRepository.save(entry);
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value);
        return s.length() > MAX ? s.substring(0, MAX) : s;
    }
}