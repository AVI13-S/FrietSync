package com.frietsync.backend.dto.chat;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChatAttachmentPageResponse {

    private List<ChatAttachmentResponse> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
