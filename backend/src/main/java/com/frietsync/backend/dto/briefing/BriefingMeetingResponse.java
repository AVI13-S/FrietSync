package com.frietsync.backend.dto.briefing;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class BriefingMeetingResponse {

    private UUID meetingId;
    private UUID projectId;
    private String title;
    private String agenda;
    private Instant startAt;
    private Instant endAt;
}