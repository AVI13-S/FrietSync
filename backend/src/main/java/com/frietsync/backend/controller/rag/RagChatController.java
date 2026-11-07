package com.frietsync.backend.controller.rag;

import com.frietsync.backend.dto.rag.RagChatRequest;
import com.frietsync.backend.dto.rag.RagChatResponse;
import com.frietsync.backend.service.rag.RagChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/rag-chat")
@RequiredArgsConstructor
public class RagChatController {

    private final RagChatService ragChatService;

    @PostMapping("/ask")
    public RagChatResponse ask(@PathVariable UUID projectId,
                               @Valid @RequestBody RagChatRequest request,
                               Authentication authentication) {
        return ragChatService.askRagChatbot(projectId, request.getQuestion(), currentUserId(authentication));
    }

    private UUID currentUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}