package com.frietsync.backend.dto.rag;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RagChatResponse {

    private String answer;
    private List<RagSourceResponse> sources = new ArrayList<>();
}