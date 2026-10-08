package com.frietsync.backend.service.impl.project;

import com.frietsync.backend.entity.issue.Issue;
import com.frietsync.backend.entity.project.Project;
import com.frietsync.backend.entity.project.ProjectMember;
import com.frietsync.backend.entity.user.Role;
import com.frietsync.backend.exception.ForbiddenException;
import com.frietsync.backend.exception.ResourceNotFoundException;
import com.frietsync.backend.repository.issue.IssueRepository;
import com.frietsync.backend.repository.project.ProjectMemberRepository;
import com.frietsync.backend.repository.project.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProjectAccess {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final IssueRepository issueRepository;

    public Project project(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }

    public Issue issue(UUID issueId) {
        return issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found"));
    }

    public Role roleOf(Project project, UUID userId) {
        if (userId.equals(project.getAdminId())) {
            return Role.ADMIN;
        }
        if (userId.equals(project.getProjectManagerId())) {
            return Role.PROJECT_MANAGER;
        }
        if (userId.equals(project.getTeamLeadId())) {
            return Role.TEAM_LEAD;
        }
        return projectMemberRepository.findByProjectIdAndUserId(project.getId(), userId)
                .map(ProjectMember::getRole)
                .orElse(null);
    }

    public Role requireMember(Project project, UUID userId) {
        Role role = roleOf(project, userId);
        if (role == null) {
            throw new ForbiddenException("You don't have access to this project");
        }
        return role;
    }

    public Role require(Project project, UUID userId, Role... allowed) {
        Role role = requireMember(project, userId);
        if (role == Role.ADMIN) {
            return role;
        }
        for (Role candidate : allowed) {
            if (candidate == role) {
                return role;
            }
        }
        throw new ForbiddenException("You don't have permission to do this");
    }
}