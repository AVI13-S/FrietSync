package com.frietsync.backend.dto.budget;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CostReportResponse {

    private UUID projectId;
    private BigDecimal totalCost;
    private List<CostLineResponse> byCategory;
    private List<MemberCostResponse> byMember;
    private List<SprintCostResponse> bySprint;
}