package com.frietsync.backend.service.impl.chat;

import com.frietsync.backend.dto.chat.ChatMuteResponse;
import com.frietsync.backend.entity.chat.ChatMute;
import com.frietsync.backend.exception.BadRequestException;
import com.frietsync.backend.repository.chat.ChatMuteRepository;
import com.frietsync.backend.service.chat.ChatAccessService;
import com.frietsync.backend.service.chat.ChatMuteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatMuteServiceImpl implements ChatMuteService {

    private final ChatMuteRepository repository;
    private final ChatAccessService chatAccessService;

    @Override
    public ChatMuteResponse mute(UUID projectId, UUID userId, Instant mutedUntil) {
        chatAccessService.assertProjectAccess(projectId, userId);
        if (mutedUntil != null && !mutedUntil.isAfter(Instant.now())) {
            throw new BadRequestException("Mute end time must be in the future");
        }
        ChatMute mute = repository.findByProjectIdAndUserId(projectId, userId).orElseGet(() -> {
            ChatMute created = new ChatMute();
            created.setProjectId(projectId);
            created.setUserId(userId);
            return created;
        });
        mute.setMutedUntil(mutedUntil);
        ChatMute saved = repository.save(mute);
        return new ChatMuteResponse(true, saved.getMutedUntil());
    }

    @Override
    public ChatMuteResponse unmute(UUID projectId, UUID userId) {
        chatAccessService.assertProjectAccess(projectId, userId);
        repository.deleteByProjectIdAndUserId(projectId, userId);
        return new ChatMuteResponse(false, null);
    }

    @Override
    @Transactional(readOnly = true)
    public ChatMuteResponse status(UUID projectId, UUID userId) {
        chatAccessService.assertProjectAccess(projectId, userId);
        return repository.findByProjectIdAndUserId(projectId, userId)
                .filter(m -> m.getMutedUntil() == null || m.getMutedUntil().isAfter(Instant.now()))
                .map(m -> new ChatMuteResponse(true, m.getMutedUntil()))
                .orElse(new ChatMuteResponse(false, null));
    }
}