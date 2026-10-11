package com.frietsync.backend.dto.notification;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class UpdatePreferenceRequest {

    @NotEmpty(message = "At least one preference is required")
    @Valid
    private List<PreferenceUpdate> preferences;
}
