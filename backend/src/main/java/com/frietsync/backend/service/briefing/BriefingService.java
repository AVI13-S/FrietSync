package com.frietsync.backend.service.briefing;

import com.frietsync.backend.dto.briefing.BriefingPreferenceResponse;
import com.frietsync.backend.dto.briefing.DailyBriefingResponse;
import com.frietsync.backend.dto.briefing.UpdateBriefingPreferenceRequest;

import java.time.LocalDate;
import java.util.UUID;

public interface BriefingService {

    DailyBriefingResponse today(UUID userId);

    DailyBriefingResponse forDate(UUID userId, LocalDate date);

    BriefingPreferenceResponse getPreferences(UUID userId);

    BriefingPreferenceResponse updatePreferences(UUID userId, UpdateBriefingPreferenceRequest request);
}