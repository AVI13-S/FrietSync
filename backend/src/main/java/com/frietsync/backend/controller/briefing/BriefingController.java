package com.frietsync.backend.controller.briefing;

import com.frietsync.backend.dto.briefing.BriefingPreferenceResponse;
import com.frietsync.backend.dto.briefing.DailyBriefingResponse;
import com.frietsync.backend.dto.briefing.UpdateBriefingPreferenceRequest;
import com.frietsync.backend.exception.BadRequestException;
import com.frietsync.backend.service.briefing.BriefingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BriefingController {

    private final BriefingService briefingService;

    @GetMapping("/briefing/today")
    public ResponseEntity<DailyBriefingResponse> today(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(briefingService.today(userId));
    }

    @GetMapping("/briefing")
    public ResponseEntity<DailyBriefingResponse> byDate(@RequestParam(required = false) String date,
                                                        @AuthenticationPrincipal UUID userId) {
        if (date == null || date.isBlank()) {
            throw new BadRequestException("Date is required, use the format 2026-10-10");
        }
        LocalDate day;
        try {
            day = LocalDate.parse(date);
        } catch (DateTimeParseException ex) {
            throw new BadRequestException("Date must look like 2026-10-10");
        }
        return ResponseEntity.ok(briefingService.forDate(userId, day));
    }

    @GetMapping("/briefing/preferences")
    public ResponseEntity<BriefingPreferenceResponse> getPreferences(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(briefingService.getPreferences(userId));
    }

    @PutMapping("/briefing/preferences")
    public ResponseEntity<BriefingPreferenceResponse> updatePreferences(
            @Valid @RequestBody UpdateBriefingPreferenceRequest request,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(briefingService.updatePreferences(userId, request));
    }
}