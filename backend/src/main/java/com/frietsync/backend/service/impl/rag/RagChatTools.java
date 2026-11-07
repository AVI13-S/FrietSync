package com.frietsync.backend.service.impl.rag;

import com.frietsync.backend.entity.issue.Issue;
import com.frietsync.backend.entity.issue.IssueStatus;
import com.frietsync.backend.entity.project.Project;
import com.frietsync.backend.repository.issue.IssueRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Locale;

public class RagChatTools {

    private static final int MAX_ISSUES = 8;

    private final Project project;
    private final IssueRepository issueRepository;

    public RagChatTools(Project project, IssueRepository issueRepository) {
        this.project = project;
        this.issueRepository = issueRepository;
    }

    @Tool(description = "Search this authorized project for issues matching the user's text query. "
            + "Use an empty status for no status filter.")
    public String searchIssues(
            @ToolParam(description = "A short search phrase describing the relevant issues") String query,
            @ToolParam(description = "Optional issue status: OPEN, IN_PROGRESS, RESOLVED, REOPENED, or CLOSED; "
                    + "leave empty to include every status") String status) {
        if (query == null || query.isBlank()) {
            return "A search phrase is required.";
        }

        IssueStatus statusFilter = null;
        if (status != null && !status.isBlank()) {
            try {
                statusFilter = IssueStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                return "Invalid status. Use OPEN, IN_PROGRESS, RESOLVED, REOPENED, or CLOSED.";
            }
        }

        List<Issue> issues = issueRepository.searchProjectIssues(
                project.getId(), query.trim(), statusFilter, PageRequest.of(0, MAX_ISSUES));
        if (issues.isEmpty()) {
            return "No matching issues were found in this project.";
        }

        StringBuilder result = new StringBuilder();
        for (Issue issue : issues) {
            result.append("- ").append(issue.getTitle()).append(" [")
                    .append(issue.getStatus()).append(", ").append(issue.getPriority()).append("]");
            if (issue.getDescription() != null && !issue.getDescription().isBlank()) {
                result.append(": ").append(issue.getDescription());
            }
            result.append('\n');
        }
        return result.toString();
    }

    @Tool(description = "Get a live summary of this authorized project, including issue counts by status.")
    public String getProjectSummary() {
        StringBuilder result = new StringBuilder()
                .append("Project: ").append(project.getName())
                .append("\nStatus: ").append(project.getStatus())
                .append("\nDescription: ")
                .append(project.getDescription() == null ? "" : project.getDescription())
                .append("\nTotal active issues: ")
                .append(issueRepository.countByProjectIdAndDeletedAtIsNull(project.getId()));
        for (IssueStatus status : IssueStatus.values()) {
            result.append("\n").append(status).append(": ")
                    .append(issueRepository.countByProjectIdAndStatusAndDeletedAtIsNull(project.getId(), status));
        }
        return result.toString();
    }
}
