package com.frietsync.backend.dto.budget;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CostLineResponse {

    private String category;
    private BigDecimal amount;
}