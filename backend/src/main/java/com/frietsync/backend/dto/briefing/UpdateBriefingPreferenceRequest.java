package com.frietsync.backend.dto.briefing;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalTime;

@Data
public class UpdateBriefingPreferenceRequest {

    private LocalTime deliveryTime;

    @Size(max = 50, message = "Time zone must be at most 50 characters")
    private String timeZone;

    private Boolean emailEnabled;
}