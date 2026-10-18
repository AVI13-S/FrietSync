package com.frietsync.backend.dto.poll;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClosePollRequest {

    @Size(max = 4000)
    private String decisionRecord;
}
