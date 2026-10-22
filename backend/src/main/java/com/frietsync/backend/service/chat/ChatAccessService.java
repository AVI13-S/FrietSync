package com.frietsync.backend.service.chat;

import com.frietsync.backend.entity.project.Project;

import java.util.UUID;

public interface ChatAccessService {

    void assertProjectAccess(UUID projectId, UUID userId);

    boolean hasProjectAccess(Project project, UUID userId);
}
