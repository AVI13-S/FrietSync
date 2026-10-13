package com.frietsync.backend.service.impl.project;

import com.frietsync.backend.dto.project.AttachIssuesRequest;
import com.frietsync.backend.dto.project.ReleaseRequest;
import com.frietsync.backend.dto.project.ReleaseResponse;
import com.frietsync.backend.entity.project.Project;
import com.frietsync.backend.entity.project.ProjectMember;
import com.frietsync.backend.entity.project.Release;
import com.frietsync.backend.entity.project.ReleaseIssue;
import com.frietsync.backend.exception.ForbiddenException;
import com.frietsync.backend.exception.ResourceNotFoundException;
import com.frietsync.backend.repository.project.ProjectMemberRepository;
import com.frietsync.backend.repository.project.ProjectRepository;
import com.frietsync.backend.repository.project.ReleaseIssueRepository;
import com.frietsync.backend.repository.project.ReleaseRepository;
import com.frietsync.backend.service.project.ReleaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReleaseServiceImpl implements ReleaseService {
    private final ReleaseRepository releaseRepository;
    private final ReleaseIssueRepository releaseIssueRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    @Override
    @Transactional
    public ReleaseResponse create(UUID projectId, UUID userId, ReleaseRequest request) {
        Project project = findProject(projectId);
        requireManager(project, userId);
        Release release = new Release();
        release.setProjectId(projectId);
        apply(release, request);
        return response(releaseRepository.save(release));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReleaseResponse> list(UUID projectId, UUID userId) {
        Project project = findProject(projectId);
        requireMember(project, userId);
        return releaseRepository.findByProjectIdOrderByReleaseDateAsc(projectId).stream()
                .map(release -> response(release)).toList();
    }

    @Override
    @Transactional
    public ReleaseResponse update(UUID releaseId, UUID userId, ReleaseRequest request) {
        Release release = findRelease(releaseId);
        requireManager(findProject(release.getProjectId()), userId);
        apply(release, request);
        return response(releaseRepository.save(release));
    }

    @Override
    @Transactional
    public void delete(UUID releaseId, UUID userId) {
        Release release = findRelease(releaseId);
        requireManager(findProject(release.getProjectId()), userId);
        releaseIssueRepository.deleteByReleaseId(releaseId);
        releaseRepository.delete(release);
    }

    @Override
    @Transactional
    public ReleaseResponse attachIssues(UUID releaseId, UUID userId, AttachIssuesRequest request) {
        Release release = findRelease(releaseId);
        Project project = findProject(release.getProjectId());
        requireManagerOrTeamLead(project, userId);
        for (UUID issueId : request.getIssueIds()) {
            if (!releaseIssueRepository.existsByReleaseIdAndIssueId(releaseId, issueId)) {
                ReleaseIssue link = new ReleaseIssue();
                link.setReleaseId(releaseId);
                link.setIssueId(issueId);
                releaseIssueRepository.save(link);
            }
        }
        return response(release);
    }

    private void apply(Release release, ReleaseRequest request) {
        release.setName(request.getName().trim());
        release.setDescription(request.getDescription());
        release.setReleaseDate(request.getReleaseDate());
    }

    private ReleaseResponse response(Release release) {
        List<UUID> issueIds = releaseIssueRepository.findByReleaseId(release.getId()).stream()
                .map(issue -> issue.getIssueId()).toList();
        return ReleaseResponse.from(release, issueIds);
    }

    private Project findProject(UUID id) {
        Optional<Project> project = projectRepository.findById(id);
        if (project.isEmpty()) {
            throw new ResourceNotFoundException("Project not found");
        }
        return project.get();
    }

    private Release findRelease(UUID id) {
        Optional<Release> release = releaseRepository.findById(id);
        if (release.isEmpty()) {
            throw new ResourceNotFoundException("Release not found");
        }
        return release.get();
    }

    private void requireManager(Project project, UUID userId) {
        if (userId == null || !userId.equals(project.getProjectManagerId())) {
            throw new ForbiddenException("Only the project manager can manage releases");
        }
    }

    private void requireManagerOrTeamLead(Project project, UUID userId) {
        if (userId == null || !(userId.equals(project.getProjectManagerId())
                || userId.equals(project.getTeamLeadId()))) {
            throw new ForbiddenException("Only the project manager or team lead can attach issues");
        }
    }

    private void requireMember(Project project, UUID userId) {
        boolean leader = userId != null && (userId.equals(project.getCreatedBy())
                || userId.equals(project.getAdminId()) || userId.equals(project.getProjectManagerId())
                || userId.equals(project.getTeamLeadId()));
        ProjectMember member = null;
        if (userId != null) {
            Optional<ProjectMember> memberResult = projectMemberRepository
                    .findByProjectIdAndUserId(project.getId(), userId);
            if (memberResult.isPresent()) {
                member = memberResult.get();
            }
        }
        if (!leader && member == null) {
            throw new ForbiddenException("You don't have access to this project's releases");
        }
    }
}
