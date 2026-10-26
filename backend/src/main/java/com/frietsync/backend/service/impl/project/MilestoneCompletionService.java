package com.frietsync.backend.service.impl.project;

import com.frietsync.backend.entity.project.Milestone;
import com.frietsync.backend.entity.sprint.Sprint;
import com.frietsync.backend.entity.sprint.SprintStatus;
import com.frietsync.backend.repository.project.MilestoneRepository;
import com.frietsync.backend.repository.sprint.SprintRepository;
import com.frietsync.backend.service.impl.activity.ProjectActivityRecorder;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MilestoneCompletionService {

    private final MilestoneRepository milestoneRepository;
    private final SprintRepository sprintRepository;
    private final ProjectActivityRecorder activityRecorder;

    @Transactional
    public void refresh(UUID milestoneId, UUID actorId) {
        if (milestoneId == null) {
            return;
        }
        Milestone milestone = milestoneRepository.findById(milestoneId).orElse(null);
        if (milestone == null) {
            return;
        }

        List<Sprint> linkedSprints = sprintRepository.findByMilestoneId(milestoneId);
        boolean complete = !linkedSprints.isEmpty()
                && linkedSprints.stream().allMatch(sprint -> sprint.getStatus() == SprintStatus.COMPLETED);
        if (complete && milestone.getCompletedAt() == null) {
            milestone.setCompletedAt(actorId == null
                    ? linkedSprints.stream().map(Sprint::getUpdatedAt).filter(java.util.Objects::nonNull)
                            .max(Comparator.naturalOrder()).orElse(Instant.now())
                    : Instant.now());
            milestoneRepository.save(milestone);
            if (actorId != null) {
                activityRecorder.record(milestone.getProjectId(), actorId, "MILESTONE", milestone.getId(),
                        milestone.getName(), "COMPLETED");
            }
        } else if (!complete && milestone.getCompletedAt() != null) {
            milestone.setCompletedAt(null);
            milestoneRepository.save(milestone);
        }
    }

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void synchronizeExistingMilestones(ApplicationReadyEvent event) {
        for (Milestone milestone : milestoneRepository.findAll()) {
            refresh(milestone.getId(), null);
        }
    }
}
