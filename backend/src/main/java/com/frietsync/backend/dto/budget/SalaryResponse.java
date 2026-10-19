package com.frietsync.backend.dto.budget;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class SalaryResponse {

    private UUID userId;
    private BigDecimal monthlySalary;
}