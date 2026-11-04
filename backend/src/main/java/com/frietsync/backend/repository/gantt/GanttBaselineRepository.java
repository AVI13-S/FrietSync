package com.frietsync.backend.repository.gantt;

import com.frietsync.backend.entity.gantt.GanttBaseline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface GanttBaselineRepository extends JpaRepository<GanttBaseline, UUID> {

    List<GanttBaseline> findByProjectId(UUID projectId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from GanttBaseline b where b.projectId = :projectId")
    void deleteAllForProject(@Param("projectId") UUID projectId);
}