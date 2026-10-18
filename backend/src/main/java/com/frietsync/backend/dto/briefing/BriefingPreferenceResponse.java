package com.frietsync.backend.dto.briefing;

import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;

@Data
@Builder
public class BriefingPreferenceResponse {

    private LocalTime deliveryTime;
    private String timeZone;
    private boolean emailEnabled;
}