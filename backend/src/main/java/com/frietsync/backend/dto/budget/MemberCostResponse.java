package com.frietsync.backend.dto.budget;

import com.frietsync.backend.entity.user.Role;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class MemberCostResponse {

    private UUID userId;
    private String name;
    private Role role;
    private BigDecimal monthlySalary;
    private long daysOnProject;
    private BigDecimal salaryCost;
}