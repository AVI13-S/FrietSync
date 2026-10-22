package com.frietsync.backend.service.impl.chat;

import com.frietsync.backend.entity.project.Project;
import com.frietsync.backend.exception.ForbiddenException;
import com.frietsync.backend.exception.ResourceNotFoundException;
import com.frietsync.backend.repository.project.ProjectMemberRepository;
import com.frietsync.backend.repository.project.ProjectRepository;
import com.frietsync.backend.service.chat.ChatAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatAccessServiceImpl implements ChatAccessService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    @Override
    @Transactional(readOnly = true)
    public void assertProjectAccess(UUID projectId, UUID userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        if (!hasProjectAccess(project, userId)) {
            throw new ForbiddenException("You don't have access to this project's chat");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasProjectAccess(Project project, UUID userId) {
        boolean projectRole = userId.equals(project.getCreatedBy())
                || userId.equals(project.getAdminId())
                || userId.equals(project.getProjectManagerId())
                || userId.equals(project.getTeamLeadId());
        return projectRole || projectMemberRepository.existsByProjectIdAndUserId(project.getId(), userId);
    }
}
