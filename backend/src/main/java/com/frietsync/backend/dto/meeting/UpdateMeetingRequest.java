package com.frietsync.backend.dto.meeting;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
public class UpdateMeetingRequest {

    @Size(min = 1, max = 200)
    private String title;

    @Size(min = 1, max = 4000)
    private String agenda;

    private Instant startAt;

    private Instant endAt;

    private List<UUID> attendeeIds;
}
