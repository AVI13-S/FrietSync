package com.frietsync.backend.dto.project;

import com.frietsync.backend.entity.project.Release;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class ReleaseResponse {
    private UUID id;
    private UUID projectId;
    private String name;
    private String description;
    private LocalDate releaseDate;
    private String releaseUrl;
    private String releaseFileUrl;
    private List<UUID> issueIds;
    private Instant createdAt;
    private Instant updatedAt;

    public static ReleaseResponse from(Release release, List<UUID> issueIds) {
        ReleaseResponse response = new ReleaseResponse();
        response.setId(release.getId());
        response.setProjectId(release.getProjectId());
        response.setName(release.getName());
        response.setDescription(release.getDescription());
        response.setReleaseDate(release.getReleaseDate());
        response.setReleaseUrl(release.getReleaseUrl());
        response.setIssueIds(issueIds);
        response.setCreatedAt(release.getCreatedAt());
        response.setUpdatedAt(release.getUpdatedAt());
        return response;
    }
}
