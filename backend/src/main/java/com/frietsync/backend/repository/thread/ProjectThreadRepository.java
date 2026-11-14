package com.frietsync.backend.repository.thread;

import com.frietsync.backend.entity.thread.ProjectThread;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProjectThreadRepository extends JpaRepository<ProjectThread, UUID> {

    Page<ProjectThread> findByProjectIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID projectId, Pageable pageable);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update ProjectThread t set t.upvotes = t.upvotes + 1 where t.id = :id")
    int incrementUpvotes(@Param("id") UUID id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update ProjectThread t set t.upvotes = t.upvotes - 1 where t.id = :id and t.upvotes > 0")
    int decrementUpvotes(@Param("id") UUID id);
}