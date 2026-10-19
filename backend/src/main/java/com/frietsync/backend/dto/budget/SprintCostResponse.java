package com.frietsync.backend.dto.budget;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class SprintCostResponse {

    private UUID sprintId;
    private Integer sprintNumber;
    private String name;
    private BigDecimal salaryCost;
    private BigDecimal expenseCost;
    private BigDecimal totalCost;
}