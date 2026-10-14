package com.frietsync.backend.dto.meeting;

import com.frietsync.backend.entity.meeting.MeetingAttendee;

import java.util.UUID;

public record MeetingAttendeeResponse(UUID userId) {

    public static MeetingAttendeeResponse fromEntity(MeetingAttendee attendee) {
        return new MeetingAttendeeResponse(attendee.getUserId());
    }
}
