package com.frietsync.backend.service.impl.issue;

import com.frietsync.backend.entity.issue.IssueHistory;
import com.frietsync.backend.entity.issue.IssueHistoryAction;
import com.frietsync.backend.repository.issue.IssueHistoryRepository;
import com.frietsync.backend.repository.issue.IssueRepository;
import com.frietsync.backend.service.activity.ProjectActivityUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class IssueHistoryRecorder {

    private static final int MAX = 1000;

    private final IssueHistoryRepository historyRepository;
    private final IssueRepository issueRepository;
    private final ApplicationEventPublisher events;

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
        if (isRecentActivity(action, field, newValue)) {
            issueRepository.findById(issueId).ifPresent(issue ->
                    events.publishEvent(new ProjectActivityUpdatedEvent(issue.getProjectId())));
        }
    }

    private boolean isRecentActivity(IssueHistoryAction action, String field, Object newValue) {
        if (action == IssueHistoryAction.CREATED || action == IssueHistoryAction.RESOLVED
                || action == IssueHistoryAction.REVIEW_APPROVED) {
            return true;
        }
        if (action != IssueHistoryAction.UPDATED || "assignees".equals(field)) {
            return false;
        }
        return !"status".equals(field) || "RESOLVED".equals(String.valueOf(newValue))
                || "CLOSED".equals(String.valueOf(newValue));
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value);
        return s.length() > MAX ? s.substring(0, MAX) : s;
    }
}
