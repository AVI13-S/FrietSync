package com.frietsync.backend.dto.budget;

import com.frietsync.backend.entity.budget.ExpenseCategory;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class ExpenseResponse {

    private UUID id;
    private UUID projectId;
    private ExpenseCategory category;
    private String title;
    private BigDecimal amount;
    private LocalDate expenseDate;
    private UUID sprintId;
    private UUID createdBy;
    private Instant createdAt;
    private Instant updatedAt;
}