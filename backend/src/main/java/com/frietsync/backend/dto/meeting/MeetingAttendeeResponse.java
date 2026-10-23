package com.frietsync.backend.dto.meeting;

import com.frietsync.backend.entity.meeting.MeetingAttendee;

import java.util.UUID;

public record MeetingAttendeeResponse(UUID userId, String name, String email) {

    public static MeetingAttendeeResponse fromEntity(MeetingAttendee attendee, String name, String email) {
        return new MeetingAttendeeResponse(attendee.getUserId(), name, email);
    }
}
