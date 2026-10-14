package com.frietsync.backend.dto.project;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class AttachIssuesRequest {
    @NotEmpty
    private List<@NotNull UUID> issueIds;
}
