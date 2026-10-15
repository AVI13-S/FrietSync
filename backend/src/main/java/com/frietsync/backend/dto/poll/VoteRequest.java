package com.frietsync.backend.dto.poll;

import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class VoteRequest {
    private UUID optionId;

    @Size(max = 20)
    private List<UUID> optionIds;
}
