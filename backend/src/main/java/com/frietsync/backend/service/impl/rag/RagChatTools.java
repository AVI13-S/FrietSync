package com.frietsync.backend.service.impl.rag;

import com.frietsync.backend.dto.rag.RagSourceResponse;
import com.frietsync.backend.entity.issue.Issue;
import com.frietsync.backend.entity.issue.IssueStatus;
import com.frietsync.backend.entity.project.Milestone;
import com.frietsync.backend.entity.project.Project;
import com.frietsync.backend.entity.project.ProjectMember;
import com.frietsync.backend.entity.sprint.Sprint;
import com.frietsync.backend.entity.user.Role;
import com.frietsync.backend.entity.user.User;
import com.frietsync.backend.entity.workspace.Workspace;
import com.frietsync.backend.repository.issue.IssueRepository;
import com.frietsync.backend.repository.issue.SemanticIssueSearchResult;
import com.frietsync.backend.repository.project.MilestoneRepository;
import com.frietsync.backend.repository.project.ProjectMemberRepository;
import com.frietsync.backend.repository.project.ProjectRepository;
import com.frietsync.backend.repository.sprint.SprintRepository;
import com.frietsync.backend.repository.sprint.SprintSemanticSearchResult;
import com.frietsync.backend.repository.user.UserRepository;
import com.frietsync.backend.repository.workspace.WorkspaceRepository;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class RagChatTools {

    private static final int MAX_ISSUES = 8;
    private static final int MAX_SPRINTS = 10;
    private static final int MAX_MILESTONES = 25;
    private static final int MAX_PROJECT_MEMBERS = 50;
    private static final int MAX_WORKSPACE_MEMBERS = 50;
    private static final int MAX_TEXT_LENGTH = 300;

    private final UUID workspaceId;
    private final UUID targetProjectId; // null if searching across all user projects
    private final UUID userId;
    private final ProjectRepository projectRepository;
    private final IssueRepository issueRepository;
    private final MilestoneRepository milestoneRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final SprintRepository sprintRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final EmbeddingModel embeddingModel;
    private final double minSimilarity;

    // the issues and sprints the tools returned, shown to the user as sources
    private final List<RagSourceResponse> sources = new ArrayList<>();

    public RagChatTools(UUID workspaceId, UUID targetProjectId, UUID userId,
                        ProjectRepository projectRepository, IssueRepository issueRepository,
                        MilestoneRepository milestoneRepository, ProjectMemberRepository projectMemberRepository,
                        SprintRepository sprintRepository, UserRepository userRepository,
                        WorkspaceRepository workspaceRepository,
                        EmbeddingModel embeddingModel, double minSimilarity) {
        this.workspaceId = workspaceId;
        this.targetProjectId = targetProjectId;
        this.userId = userId;
        this.projectRepository = projectRepository;
        this.issueRepository = issueRepository;
        this.milestoneRepository = milestoneRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.sprintRepository = sprintRepository;
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.embeddingModel = embeddingModel;
        this.minSimilarity = minSimilarity;
    }

    public List<RagSourceResponse> getSources() {
        return sources;
    }

    @Tool(description = "Search authorized projects for issues matching the user's text query. "
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

        List<Issue> issues;
        if (targetProjectId != null) {
            issues = issueRepository.searchProjectIssues(
                    targetProjectId, query.trim(), statusFilter, PageRequest.of(0, MAX_ISSUES));
        } else {
            // Search across all projects user has access to in workspace
            issues = new ArrayList<>();
            List<Project> projects = projectRepository.findAllForUser(userId);
            for (Project p : projects) {
                if (workspaceId.equals(p.getWorkspaceId())) {
                    issues.addAll(issueRepository.searchProjectIssues(
                            p.getId(), query.trim(), statusFilter, PageRequest.of(0, MAX_ISSUES)));
                }
                if (issues.size() >= MAX_ISSUES) break;
            }
        }

        if (issues.isEmpty()) {
            return "No matching issues were found in your projects.";
        }
        return formatIssues(issues);
    }

    @Tool(description = "Find issues across authorized projects whose meaning is similar to the text, even when "
            + "the exact words differ. Use this when the user describes a problem in their own words.")
    public String searchIssuesSemantically(
            @ToolParam(description = "A short description of the topic or problem") String query) {
        if (query == null || query.isBlank()) {
            return "A search phrase is required.";
        }

        String vector;
        try {
            vector = formatVector(embeddingModel.embed(query.trim()));
        } catch (RuntimeException ex) {
            return "Semantic search is not available right now. Use the keyword issue search instead.";
        }

        String projParam = targetProjectId != null ? targetProjectId.toString() : null;
        List<SemanticIssueSearchResult> rows =
                issueRepository.searchSemanticallyForUser(userId, vector, projParam, null, MAX_ISSUES);
        StringBuilder result = new StringBuilder();
        for (SemanticIssueSearchResult row : rows) {
            if (row.getSimilarity() < minSimilarity) {
                continue;
            }
            result.append("- [").append(row.getProjectName()).append("] ").append(row.getTitle()).append(" [")
                    .append(row.getStatus()).append(", ").append(row.getPriority()).append("] (id: ")
                    .append(row.getIssueId()).append(")");
            if (row.getDescription() != null && !row.getDescription().isBlank()) {
                result.append(": ").append(shorten(row.getDescription()));
            }
            result.append('\n');
            addSource("ISSUE", row.getIssueId(), row.getProjectId(), row.getProjectName(), row.getTitle());
        }
        if (result.length() == 0) {
            return "No similar issues were found in your projects.";
        }
        return result.toString();
    }

    @Tool(description = "Get the details of one issue across authorized projects, including who is assigned "
            + "to it. Use the issue id from an earlier search result.")
    public String getIssueDetails(
            @ToolParam(description = "The issue id (a UUID) from a search result") String issueId) {
        if (issueId == null || issueId.isBlank()) {
            return "An issue id is required.";
        }
        UUID id;
        try {
            id = UUID.fromString(issueId.trim());
        } catch (IllegalArgumentException ex) {
            return "That is not a valid issue id. Search for the issue first to get its id.";
        }

        Issue issue = issueRepository.findById(id).orElse(null);
        if (issue == null || issue.getDeletedAt() != null) {
            return "That issue was not found.";
        }

        Project project = projectRepository.findById(issue.getProjectId()).orElse(null);
        if (project == null || !workspaceId.equals(project.getWorkspaceId())) {
            return "That issue was not found.";
        }

        List<String> assignees = issueRepository.findAssigneeNames(id);
        String assignedTo = "nobody";
        if (!assignees.isEmpty()) {
            assignedTo = String.join(", ", assignees);
        }

        addSource("ISSUE", issue.getId(), project.getId(), project.getName(), issue.getTitle());
        StringBuilder result = new StringBuilder()
                .append("Project: ").append(project.getName())
                .append("\nTitle: ").append(issue.getTitle())
                .append("\nStatus: ").append(issue.getStatus())
                .append("\nPriority: ").append(issue.getPriority())
                .append("\nAssigned to: ").append(assignedTo);
        if (issue.getDescription() != null && !issue.getDescription().isBlank()) {
            result.append("\nDescription: ").append(issue.getDescription());
        }
        return result.toString();
    }

    @Tool(description = "List the issues across authorized projects that are assigned to the user who is "
            + "asking, newest activity first. Use it for questions like 'what am I working on'.")
    public String getMyAssignedIssues() {
        List<Issue> issues = issueRepository.findAssignedToUserInWorkspace(userId, workspaceId);
        if (issues.isEmpty()) {
            return "No issues are assigned to you across your projects.";
        }
        return formatIssues(issues);
    }

    @Tool(description = "List the sprints of authorized projects, newest first, with status, goal, dates and "
            + "the assigned contributor. Use an empty status for no status filter.")
    public String listSprints(
            @ToolParam(description = "Optional sprint status such as PLANNED, ACTIVE or COMPLETED; "
                    + "leave empty to include every status") String status) {
        List<Project> projects = projectRepository.findAllForUser(userId);
        StringBuilder result = new StringBuilder();
        int count = 0;

        for (Project project : projects) {
            if (!workspaceId.equals(project.getWorkspaceId())) {
                continue;
            }
            if (targetProjectId != null && !targetProjectId.equals(project.getId())) {
                continue;
            }

            List<Sprint> sprints = sprintRepository
                    .findByProjectIdAndDeletedAtIsNullOrderBySprintNumberDesc(project.getId());

            for (Sprint sprint : sprints) {
                if (count >= MAX_SPRINTS) {
                    break;
                }
                if (status != null && !status.isBlank()
                        && !sprint.getStatus().name().equalsIgnoreCase(status.trim())) {
                    continue;
                }

                String startDate = sprint.getStartDate() != null ? sprint.getStartDate().toString() : "not set";
                String endDate = sprint.getEndDate() != null ? sprint.getEndDate().toString() : "not set";
                String contributor = "nobody";
                if (sprint.getAssignedContributorId() != null) {
                    User user = userRepository.findById(sprint.getAssignedContributorId()).orElse(null);
                    if (user != null) {
                        contributor = user.getName();
                    }
                }

                result.append("- [").append(project.getName()).append("] Sprint ").append(sprint.getSprintNumber())
                        .append(": ").append(sprint.getName())
                        .append(" [").append(sprint.getStatus()).append("] (id: ").append(sprint.getId()).append(")")
                        .append(", start ").append(startDate).append(", end ").append(endDate)
                        .append(", assigned to ").append(contributor);
                if (sprint.getGoal() != null && !sprint.getGoal().isBlank()) {
                    result.append(", goal: ").append(shorten(sprint.getGoal()));
                }
                result.append('\n');
                addSource("SPRINT", sprint.getId(), project.getId(), project.getName(), sprint.getName());
                count++;
            }
            if (count >= MAX_SPRINTS) break;
        }

        if (count == 0) {
            return "No matching sprints were found in your projects.";
        }
        return result.toString();
    }

    @Tool(description = "Get a live summary of authorized projects, including issue and sprint counts.")
    public String getProjectSummary() {
        List<Project> projects = projectRepository.findAllForUser(userId);
        StringBuilder result = new StringBuilder();
        int count = 0;

        for (Project project : projects) {
            if (!workspaceId.equals(project.getWorkspaceId())) {
                continue;
            }
            if (targetProjectId != null && !targetProjectId.equals(project.getId())) {
                continue;
            }

            result.append("Project: ").append(project.getName())
                    .append("\nStatus: ").append(project.getStatus())
                    .append("\nDescription: ")
                    .append(project.getDescription() == null ? "" : project.getDescription())
                    .append("\nTotal active issues: ")
                    .append(issueRepository.countByProjectIdAndDeletedAtIsNull(project.getId()))
                    .append("\nTotal active sprints: ")
                    .append(sprintRepository.countByProjectIdAndDeletedAtIsNull(project.getId()))
                    .append("\n\n");
            count++;
        }

        if (count == 0) {
            return "No projects found.";
        }
        return result.toString();
    }

    @Tool(description = "List upcoming and completed milestones for the current project, or across the user's "
            + "authorized projects in the workspace.")
    public String listMilestones() {
        StringBuilder result = new StringBuilder();
        int count = 0;
        for (Project project : authorizedProjects()) {
            for (Milestone milestone : milestoneRepository
                    .findByProjectIdAndDeletedAtIsNullOrderByDueDateAsc(project.getId())) {
                if (count >= MAX_MILESTONES) {
                    break;
                }
                result.append("- [").append(project.getName()).append("] ")
                        .append(milestone.getName()).append(" (id: ").append(milestone.getId()).append(")")
                        .append(", due ").append(milestone.getDueDate())
                        .append(", status ")
                        .append(milestone.getCompletedAt() == null ? "INCOMPLETE" : "COMPLETED");
                if (milestone.getCompletedAt() != null) {
                    result.append(" at ").append(milestone.getCompletedAt());
                }
                if (milestone.getDescription() != null && !milestone.getDescription().isBlank()) {
                    result.append(", description: ").append(shorten(milestone.getDescription()));
                }
                result.append('\n');
                addSource("MILESTONE", milestone.getId(), project.getId(), project.getName(), milestone.getName());
                count++;
            }
            if (count >= MAX_MILESTONES) {
                break;
            }
        }
        return count == 0 ? "No milestones were found in your projects." : result.toString();
    }

    @Tool(description = "List project administrators, managers, team leads, and members with their workspace "
            + "identity and project role.")
    public String listProjectMembers() {
        StringBuilder result = new StringBuilder();
        int count = 0;
        for (Project project : authorizedProjects()) {
            if (count >= MAX_PROJECT_MEMBERS) {
                result.append("- List truncated to ").append(MAX_PROJECT_MEMBERS).append(" members.\n");
                break;
            }
            Set<UUID> included = new LinkedHashSet<>();
            count += appendProjectMember(result, project, project.getAdminId(), Role.ADMIN, included);
            if (count < MAX_PROJECT_MEMBERS) {
                count += appendProjectMember(result, project, project.getProjectManagerId(),
                        Role.PROJECT_MANAGER, included);
            }
            if (count < MAX_PROJECT_MEMBERS) {
                count += appendProjectMember(result, project, project.getTeamLeadId(), Role.TEAM_LEAD, included);
            }
            for (ProjectMember member : projectMemberRepository.findByProjectId(project.getId())) {
                if (count >= MAX_PROJECT_MEMBERS) {
                    result.append("- List truncated to ").append(MAX_PROJECT_MEMBERS).append(" members.\n");
                    return result.toString();
                }
                count += appendProjectMember(result, project, member.getUserId(), member.getRole(), included);
            }
        }
        return count == 0 ? "No project members were found in your projects." : result.toString();
    }

    @Tool(description = "Get the current workspace's name, creation date, administrator, and member roster.")
    public String getWorkspaceDetails() {
        Workspace workspace = workspaceRepository.findById(workspaceId).orElse(null);
        if (workspace == null) {
            return "Workspace details were not found.";
        }

        User admin = userRepository.findById(workspace.getAdminId()).orElse(null);
        StringBuilder result = new StringBuilder()
                .append("Workspace: ").append(workspace.getName())
                .append("\nCreated: ").append(workspace.getCreatedOn())
                .append("\nAdministrator: ")
                .append(admin == null ? "unknown" : admin.getName() + " (" + admin.getEmail() + ")")
                .append("\nMembers:\n");
        List<User> members = userRepository.findAllByWorkspaceIdOrderByCreatedAtAsc(workspaceId);
        int memberCount = Math.min(members.size(), MAX_WORKSPACE_MEMBERS);
        for (int i = 0; i < memberCount; i++) {
            User member = members.get(i);
            result.append("- ").append(member.getName()).append(" (").append(member.getEmail())
                    .append(", ").append(member.getRole()).append(")\n");
        }
        if (members.size() > MAX_WORKSPACE_MEMBERS) {
            result.append("- List truncated to ").append(MAX_WORKSPACE_MEMBERS).append(" members.\n");
        }
        addSource("WORKSPACE", workspace.getId(), null, null, workspace.getName());
        return result.toString();
    }

    private List<Project> authorizedProjects() {
        return projectRepository.findAllForUser(userId).stream()
                .filter(project -> workspaceId.equals(project.getWorkspaceId()))
                .filter(project -> targetProjectId == null || targetProjectId.equals(project.getId()))
                .toList();
    }

    private int appendProjectMember(StringBuilder result, Project project, UUID memberId, Role role,
                                    Set<UUID> included) {
        if (memberId == null || !included.add(memberId)) {
            return 0;
        }
        User member = userRepository.findById(memberId).orElse(null);
        if (member == null || !workspaceId.equals(member.getWorkspaceId())) {
            return 0;
        }
        result.append("- [").append(project.getName()).append("] ")
                .append(member.getName()).append(" (").append(member.getEmail())
                .append(", ").append(role).append(")\n");
        addSource("PROJECT_MEMBER", member.getId(), project.getId(), project.getName(), member.getName());
        return 1;
    }

    private String formatIssues(List<Issue> issues) {
        StringBuilder result = new StringBuilder();
        for (Issue issue : issues) {
            Project project = projectRepository.findById(issue.getProjectId()).orElse(null);
            String projectName = project != null ? project.getName() : "Unknown Project";
            UUID projId = project != null ? project.getId() : null;

            result.append("- [").append(projectName).append("] ").append(issue.getTitle()).append(" [")
                    .append(issue.getStatus()).append(", ").append(issue.getPriority()).append("] (id: ")
                    .append(issue.getId()).append(")");
            if (issue.getDescription() != null && !issue.getDescription().isBlank()) {
                result.append(": ").append(shorten(issue.getDescription()));
            }
            result.append('\n');
            addSource("ISSUE", issue.getId(), projId, projectName, issue.getTitle());
        }
        return result.toString();
    }

    private void addSource(String type, UUID id, UUID projectId, String projectName, String title) {
        for (RagSourceResponse existing : sources) {
            if (id.equals(existing.getId())) {
                return;
            }
        }
        RagSourceResponse source = new RagSourceResponse();
        source.setType(type);
        source.setId(id);
        source.setProjectId(projectId);
        source.setProjectName(projectName);
        source.setTitle(title);
        sources.add(source);
    }

    private String shorten(String text) {
        if (text.length() > MAX_TEXT_LENGTH) {
            return text.substring(0, MAX_TEXT_LENGTH) + "...";
        }
        return text;
    }

    private String formatVector(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
