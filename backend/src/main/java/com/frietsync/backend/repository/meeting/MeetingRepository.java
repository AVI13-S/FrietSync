package com.frietsync.backend.repository.meeting;

import com.frietsync.backend.entity.meeting.Meeting;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MeetingRepository extends JpaRepository<Meeting, UUID> {

    List<Meeting> findByProjectIdOrderByStartAtAsc(UUID projectId);

    List<Meeting> findByOrganizerIdOrderByStartAtAsc(UUID organizerId);

    List<Meeting> findByIdInOrderByStartAtAsc(List<UUID> ids);

    @Modifying
    @Query("delete from Meeting m where m.projectId = :projectId")
    void deleteByProjectId(@Param("projectId") UUID projectId);
}
