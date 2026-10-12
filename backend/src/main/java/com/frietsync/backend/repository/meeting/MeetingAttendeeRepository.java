package com.frietsync.backend.repository.meeting;

import com.frietsync.backend.entity.meeting.MeetingAttendee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MeetingAttendeeRepository extends JpaRepository<MeetingAttendee, UUID> {

    List<MeetingAttendee> findByMeetingId(UUID meetingId);

    List<MeetingAttendee> findByUserId(UUID userId);

    Optional<MeetingAttendee> findByMeetingIdAndUserId(UUID meetingId, UUID userId);

    void deleteByMeetingId(UUID meetingId);

    @Modifying
    @Query("delete from MeetingAttendee a where a.meetingId in "
            + "(select m.id from Meeting m where m.projectId = :projectId)")
    void deleteByProjectId(@Param("projectId") UUID projectId);
}
