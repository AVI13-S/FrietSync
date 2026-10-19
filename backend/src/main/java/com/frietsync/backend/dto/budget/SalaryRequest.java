package com.frietsync.backend.dto.budget;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalaryRequest {

    @NotNull(message = "Monthly salary is required")
    @PositiveOrZero(message = "Monthly salary must not be negative")
    @Digits(integer = 13, fraction = 2, message = "Monthly salary can have at most 13 digits and 2 decimals")
    private BigDecimal monthlySalary;
}