package com.frietsync.backend.service.impl.activity;

import com.frietsync.backend.entity.activity.ProjectActivity;
import com.frietsync.backend.repository.activity.ProjectActivityRepository;
import com.frietsync.backend.service.activity.ProjectActivityUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectActivityRecorder {

    private final ProjectActivityRepository repository;
    private final ApplicationEventPublisher events;

    public void record(UUID projectId, UUID actorId, String resourceType,
                       UUID resourceId, String resourceName, String action) {
        ProjectActivity activity = new ProjectActivity();
        activity.setProjectId(projectId);
        activity.setActorId(actorId);
        activity.setResourceType(resourceType);
        activity.setResourceId(resourceId);
        activity.setResourceName(resourceName);
        activity.setAction(action);
        repository.save(activity);
        events.publishEvent(new ProjectActivityUpdatedEvent(projectId));
    }

    public void recordOnce(UUID projectId, UUID actorId, String resourceType,
                           UUID resourceId, String resourceName, String action) {
        if (!repository.existsByProjectIdAndResourceTypeAndResourceIdAndAction(
                projectId, resourceType, resourceId, action)) {
            record(projectId, actorId, resourceType, resourceId, resourceName, action);
        }
    }
}
