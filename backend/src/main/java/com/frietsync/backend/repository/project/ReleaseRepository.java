package com.frietsync.backend.repository.project;

import com.frietsync.backend.entity.project.Release;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReleaseRepository extends JpaRepository<Release, UUID> {
    List<Release> findByProjectIdOrderByReleaseDateAsc(UUID projectId);
    void deleteByProjectId(UUID projectId);
}
