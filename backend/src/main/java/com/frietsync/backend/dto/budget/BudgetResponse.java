package com.frietsync.backend.dto.budget;

import com.frietsync.backend.entity.budget.BudgetStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class BudgetResponse {

    private UUID projectId;

    private BigDecimal totalBudget;

    private BigDecimal salaryCost;
    private BigDecimal expenseCost;
    private BigDecimal actualCost;
    private BigDecimal remainingBudget;
    private BigDecimal percentUsed;

    private BigDecimal dailyBurnRate;
    private int remainingIssues;
    private Long estimatedRemainingDays;
    private BigDecimal projectedFinalCost;

    private BudgetStatus status;
    private String alertMessage;

    private int membersWithoutSalary;
}