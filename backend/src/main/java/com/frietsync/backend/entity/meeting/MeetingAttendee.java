package com.frietsync.backend.entity.meeting;

import com.frietsync.backend.entity.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "meeting_attendees",
        uniqueConstraints = @UniqueConstraint(columnNames = {"meeting_id", "user_id"}))
public class MeetingAttendee extends BaseEntity {

    @Column(name = "meeting_id", nullable = false)
    private UUID meetingId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

}
