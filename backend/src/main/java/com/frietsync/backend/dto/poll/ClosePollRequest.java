package com.frietsync.backend.dto.poll;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClosePollRequest {

    private String decisionRecord;
}
