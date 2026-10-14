package com.frietsync.backend.repository.replay;

import com.frietsync.backend.entity.replay.ProjectReplayPhase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectReplayPhaseRepository extends JpaRepository<ProjectReplayPhase, UUID> {

    List<ProjectReplayPhase> findByProjectId(UUID projectId);

    void deleteByProjectId(UUID projectId);
}