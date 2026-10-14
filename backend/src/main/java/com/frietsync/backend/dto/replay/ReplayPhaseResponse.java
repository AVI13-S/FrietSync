package com.frietsync.backend.dto.replay;

import com.frietsync.backend.entity.replay.ReplayPhaseType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ReplayPhaseResponse {

    private UUID id;
    private ReplayPhaseType type;
    private String title;
    private String description;

    private Instant startAt;
    private Instant endAt;

    private int issueCount;

    private boolean reached;
}