package com.frietsync.backend.repository.activity;

import com.frietsync.backend.entity.activity.ProjectActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectActivityRepository extends JpaRepository<ProjectActivity, UUID> {

    List<ProjectActivity> findByProjectIdOrderByCreatedAtDescIdDesc(UUID projectId, Pageable pageable);

    List<ProjectActivity> findByProjectIdAndResourceTypeInOrderByCreatedAtDescIdDesc(
            UUID projectId, List<String> resourceTypes, Pageable pageable);

    boolean existsByProjectIdAndResourceTypeAndResourceIdAndAction(
            UUID projectId, String resourceType, UUID resourceId, String action);

    void deleteByProjectId(UUID projectId);
}
