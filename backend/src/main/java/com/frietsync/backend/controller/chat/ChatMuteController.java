package com.frietsync.backend.controller.chat;

import com.frietsync.backend.dto.chat.ChatMuteRequest;
import com.frietsync.backend.dto.chat.ChatMuteResponse;
import com.frietsync.backend.service.chat.ChatMuteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/chat/mute")
@RequiredArgsConstructor
public class ChatMuteController {

    private final ChatMuteService chatMuteService;

    @GetMapping
    public ChatMuteResponse status(@PathVariable UUID projectId, @AuthenticationPrincipal UUID userId) {
        return chatMuteService.status(projectId, userId);
    }

    @PutMapping
    public ChatMuteResponse mute(@PathVariable UUID projectId,
                                 @AuthenticationPrincipal UUID userId,
                                 @RequestBody(required = false) ChatMuteRequest request) {
        return chatMuteService.mute(projectId, userId, request == null ? null : request.getMutedUntil());
    }

    @DeleteMapping
    public ChatMuteResponse unmute(@PathVariable UUID projectId, @AuthenticationPrincipal UUID userId) {
        return chatMuteService.unmute(projectId, userId);
    }
}