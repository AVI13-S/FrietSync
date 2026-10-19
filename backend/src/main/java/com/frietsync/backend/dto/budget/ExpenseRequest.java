package com.frietsync.backend.dto.budget;

import com.frietsync.backend.entity.budget.ExpenseCategory;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class ExpenseRequest {

    @NotNull(message = "Category is required")
    private ExpenseCategory category;

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must be at most 200 characters")
    private String title;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be more than 0")
    @Digits(integer = 13, fraction = 2, message = "Amount can have at most 13 digits and 2 decimals")
    private BigDecimal amount;

    private LocalDate expenseDate;

    private UUID sprintId;
}