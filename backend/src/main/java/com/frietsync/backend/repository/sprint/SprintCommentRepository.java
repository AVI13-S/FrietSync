package com.frietsync.backend.repository.sprint;

import com.frietsync.backend.entity.sprint.SprintComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SprintCommentRepository extends JpaRepository<SprintComment, UUID> {
    List<SprintComment> findBySprintIdOrderByCreatedAtAsc(UUID sprintId);

    void deleteBySprintId(UUID sprintId);
}
