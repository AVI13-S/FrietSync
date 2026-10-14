package com.frietsync.backend.dto.issue;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class IssuePageResponse<T> {
    private List<T> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
