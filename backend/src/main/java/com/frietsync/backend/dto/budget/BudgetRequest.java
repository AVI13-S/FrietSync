package com.frietsync.backend.dto.budget;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BudgetRequest {

    @NotNull(message = "Total budget is required")
    @Positive(message = "Total budget must be more than 0")
    @Digits(integer = 13, fraction = 2, message = "Total budget can have at most 13 digits and 2 decimals")
    private BigDecimal totalBudget;
}