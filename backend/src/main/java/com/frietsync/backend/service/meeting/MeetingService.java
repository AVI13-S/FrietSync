package com.frietsync.backend.service.meeting;

import com.frietsync.backend.dto.meeting.CreateMeetingRequest;
import com.frietsync.backend.dto.meeting.MeetingResponse;
import com.frietsync.backend.dto.meeting.UpdateMeetingRequest;

import java.util.List;
import java.util.UUID;

public interface MeetingService {

    MeetingResponse create(UUID projectId, UUID organizerId, CreateMeetingRequest request);

    List<MeetingResponse> getMyMeetings(UUID userId);

    List<MeetingResponse> getProjectMeetings(UUID projectId, UUID userId);

    MeetingResponse update(UUID meetingId, UUID userId, UpdateMeetingRequest request);

    void cancel(UUID meetingId, UUID userId);

}
