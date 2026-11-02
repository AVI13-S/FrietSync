package com.frietsync.backend.service.impl.canvas;

import com.frietsync.backend.dto.canvas.CanvasDetailResponse;
import com.frietsync.backend.dto.canvas.CanvasElementRequest;
import com.frietsync.backend.dto.canvas.CanvasElementResponse;
import com.frietsync.backend.dto.canvas.CanvasElementUpdateRequest;
import com.frietsync.backend.dto.canvas.CanvasEvent;
import com.frietsync.backend.dto.canvas.CanvasEventType;
import com.frietsync.backend.dto.canvas.CanvasResponse;
import com.frietsync.backend.dto.canvas.CanvasTemplateResponse;
import com.frietsync.backend.dto.canvas.ConvertToIssueRequest;
import com.frietsync.backend.dto.canvas.ConvertToIssueResponse;
import com.frietsync.backend.dto.canvas.CreateCanvasRequest;
import com.frietsync.backend.dto.canvas.RenameCanvasRequest;
import com.frietsync.backend.dto.issue.IssueRequest;
import com.frietsync.backend.dto.issue.IssueResponse;
import com.frietsync.backend.dto.issue.IssueSummaryResponse;
import com.frietsync.backend.entity.canvas.Canvas;
import com.frietsync.backend.entity.canvas.CanvasElement;
import com.frietsync.backend.entity.canvas.CanvasElementType;
import com.frietsync.backend.entity.canvas.CanvasTemplate;
import com.frietsync.backend.entity.issue.Issue;
import com.frietsync.backend.entity.project.Project;
import com.frietsync.backend.entity.user.Role;
import com.frietsync.backend.entity.user.User;
import com.frietsync.backend.exception.BadRequestException;
import com.frietsync.backend.exception.ForbiddenException;
import com.frietsync.backend.exception.ResourceNotFoundException;
import com.frietsync.backend.repository.canvas.CanvasElementRepository;
import com.frietsync.backend.repository.canvas.CanvasRepository;
import com.frietsync.backend.repository.issue.IssueRepository;
import com.frietsync.backend.repository.user.UserRepository;
import com.frietsync.backend.service.canvas.CanvasService;
import com.frietsync.backend.service.impl.project.ProjectAccess;
import com.frietsync.backend.service.issue.IssueService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class CanvasServiceImpl implements CanvasService {

    private static final int MAX_CANVASES_PER_PROJECT = 50;
    private static final int MAX_ELEMENTS_PER_CANVAS = 2000;

    private final CanvasRepository canvasRepository;
    private final CanvasElementRepository elementRepository;
    private final IssueRepository issueRepository;
    private final IssueService issueService;
    private final UserRepository userRepository;
    private final ProjectAccess access;
    private final CanvasTemplateBuilder templateBuilder;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional(readOnly = true)
    public List<CanvasTemplateResponse> templates() {
        List<CanvasTemplateResponse> list = new ArrayList<>();
        list.add(CanvasTemplateResponse.builder().key("BLANK").name("Blank canvas")
                .description("Start with an empty canvas.").build());
        list.add(CanvasTemplateResponse.builder().key("FLOWCHART").name("Flowchart")
                .description("Start, process steps, a decision with two branches and an end.").build());
        list.add(CanvasTemplateResponse.builder().key("ARCHITECTURE").name("Architecture diagram")
                .description("Client, API gateway, services and a database.").build());
        list.add(CanvasTemplateResponse.builder().key("USER_FLOW").name("User flow")
                .description("A sign-up journey from landing page to dashboard, with a notes area.").build());
        return list;
    }

    @Override
    public CanvasDetailResponse createCanvas(UUID projectId, CreateCanvasRequest request, UUID userId) {
        requireEditor(projectId, userId);
        if (canvasRepository.countByProjectId(projectId) >= MAX_CANVASES_PER_PROJECT) {
            throw new BadRequestException("A project can have at most " + MAX_CANVASES_PER_PROJECT + " canvases");
        }

        Canvas canvas = new Canvas();
        canvas.setProjectId(projectId);
        canvas.setName(request.getName().trim());
        canvas.setCreatedBy(userId);
        canvas = canvasRepository.save(canvas);

        List<CanvasElement> created = new ArrayList<>();
        CanvasTemplate template = request.getTemplate();
        if (template != null && template != CanvasTemplate.BLANK) {
            created = templateBuilder.build(canvas.getId(), template, userId);
        }
        return buildDetail(canvas, created);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CanvasResponse> listCanvases(UUID projectId, UUID userId) {
        Project project = access.project(projectId);
        access.requireMember(project, userId);
        return toCanvasResponses(canvasRepository.findByProjectIdOrderByUpdatedAtDesc(projectId));
    }

    @Override
    @Transactional(readOnly = true)
    public CanvasDetailResponse getCanvas(UUID canvasId, UUID userId) {
        Canvas canvas = findCanvas(canvasId);
        Project project = access.project(canvas.getProjectId());
        access.requireMember(project, userId);
        List<CanvasElement> elements = elementRepository.findByCanvasIdOrderByZIndexAscCreatedAtAsc(canvasId);
        return buildDetail(canvas, elements);
    }

    @Override
    public CanvasResponse renameCanvas(UUID canvasId, RenameCanvasRequest request, UUID userId) {
        Canvas canvas = findCanvas(canvasId);
        requireEditor(canvas.getProjectId(), userId);
        canvas.setName(request.getName().trim());
        canvas = canvasRepository.save(canvas);

        events.publishEvent(CanvasEvent.builder()
                .type(CanvasEventType.CANVAS_RENAMED)
                .canvasId(canvasId)
                .actorId(userId)
                .name(canvas.getName())
                .occurredAt(Instant.now())
                .build());

        List<Canvas> one = new ArrayList<>();
        one.add(canvas);
        return toCanvasResponses(one).get(0);
    }

    @Override
    public void deleteCanvas(UUID canvasId, UUID userId) {
        Canvas canvas = findCanvas(canvasId);
        Project project = access.project(canvas.getProjectId());
        Role role = access.requireMember(project, userId);

        boolean allowed = userId.equals(canvas.getCreatedBy())
                || role == Role.ADMIN
                || role == Role.PROJECT_MANAGER;
        if (!allowed) {
            throw new ForbiddenException("Only the creator or a project manager can delete this canvas");
        }

        elementRepository.deleteByCanvasId(canvasId);
        canvasRepository.delete(canvas);

        events.publishEvent(CanvasEvent.builder()
                .type(CanvasEventType.CANVAS_DELETED)
                .canvasId(canvasId)
                .actorId(userId)
                .occurredAt(Instant.now())
                .build());
    }

    @Override
    public void deleteAllForProject(UUID projectId) {
        for (Canvas canvas : canvasRepository.findByProjectId(projectId)) {
            elementRepository.deleteByCanvasId(canvas.getId());
            canvasRepository.delete(canvas);
        }
    }

    @Override
    public CanvasElementResponse createElement(UUID canvasId, CanvasElementRequest r, UUID userId) {
        Canvas canvas = findCanvas(canvasId);
        requireEditor(canvas.getProjectId(), userId);
        if (elementRepository.countByCanvasId(canvasId) >= MAX_ELEMENTS_PER_CANVAS) {
            throw new BadRequestException("A canvas can have at most " + MAX_ELEMENTS_PER_CANVAS + " items");
        }

        CanvasElementType type = r.getType();
        CanvasElement element = newElement(canvasId, type, userId);
        element.setX(r.getX());
        element.setY(r.getY());
        if (r.getRotation() != null) {
            element.setRotation(r.getRotation());
        }
        if (r.getZIndex() != null) {
            element.setZIndex(r.getZIndex());
        } else {
            element.setZIndex(elementRepository.findMaxZIndex(canvasId) + 1);
        }
        if (r.getColor() != null) {
            element.setColor(r.getColor());
        } else {
            element.setColor(defaultColor(type));
        }
        element.setText(emptyToNull(r.getText()));

        if (type == CanvasElementType.STICKY_NOTE) {
            element.setWidth(valueOrDefault(r.getWidth(), 200));
            element.setHeight(valueOrDefault(r.getHeight(), 200));
        } else if (type == CanvasElementType.SHAPE) {
            if (r.getShape() == null) {
                throw new BadRequestException("A shape needs a shape type");
            }
            element.setShape(r.getShape());
            element.setWidth(valueOrDefault(r.getWidth(), 160));
            element.setHeight(valueOrDefault(r.getHeight(), 100));
        } else if (type == CanvasElementType.TEXT) {
            if (element.getText() == null || element.getText().trim().isEmpty()) {
                throw new BadRequestException("Text items cannot be empty");
            }
            element.setWidth(valueOrDefault(r.getWidth(), 200));
            element.setHeight(valueOrDefault(r.getHeight(), 40));
        } else if (type == CanvasElementType.ARROW) {
            if (r.getEndX() == null || r.getEndY() == null) {
                throw new BadRequestException("An arrow needs endX and endY");
            }
            element.setEndX(r.getEndX());
            element.setEndY(r.getEndY());
            element.setFromElementId(checkConnector(canvasId, r.getFromElementId()));
            element.setToElementId(checkConnector(canvasId, r.getToElementId()));
        }

        element = elementRepository.save(element);
        publishUpsert(canvasId, userId, element);
        return toElementResponse(element);
    }

    @Override
    public CanvasElementResponse updateElement(UUID canvasId, UUID elementId,
                                               CanvasElementUpdateRequest r, UUID userId) {
        Canvas canvas = findCanvas(canvasId);
        requireEditor(canvas.getProjectId(), userId);
        CanvasElement element = findElement(canvasId, elementId);

        if (r.getX() != null) {
            element.setX(r.getX());
        }
        if (r.getY() != null) {
            element.setY(r.getY());
        }
        if (r.getRotation() != null) {
            element.setRotation(r.getRotation());
        }
        if (r.getZIndex() != null) {
            element.setZIndex(r.getZIndex());
        }
        if (r.getColor() != null) {
            element.setColor(r.getColor());
        }
        if (r.getText() != null) {
            if (element.getType() == CanvasElementType.TEXT && r.getText().trim().isEmpty()) {
                throw new BadRequestException("Text items cannot be empty");
            }
            element.setText(emptyToNull(r.getText()));
        }

        if (element.getType() == CanvasElementType.ARROW) {
            if (r.getEndX() != null) {
                element.setEndX(r.getEndX());
            }
            if (r.getEndY() != null) {
                element.setEndY(r.getEndY());
            }
            if (r.isDetachFrom()) {
                element.setFromElementId(null);
            } else if (r.getFromElementId() != null) {
                element.setFromElementId(checkConnector(canvasId, r.getFromElementId()));
            }
            if (r.isDetachTo()) {
                element.setToElementId(null);
            } else if (r.getToElementId() != null) {
                element.setToElementId(checkConnector(canvasId, r.getToElementId()));
            }
        } else {
            if (r.getWidth() != null) {
                element.setWidth(r.getWidth());
            }
            if (r.getHeight() != null) {
                element.setHeight(r.getHeight());
            }
            if (element.getType() == CanvasElementType.SHAPE && r.getShape() != null) {
                element.setShape(r.getShape());
            }
        }

        element.setUpdatedBy(userId);
        element.setRevision(element.getRevision() + 1);
        element = elementRepository.save(element);
        publishUpsert(canvasId, userId, element);
        return toElementResponse(element);
    }

    @Override
    public void deleteElement(UUID canvasId, UUID elementId, UUID userId) {
        Canvas canvas = findCanvas(canvasId);
        requireEditor(canvas.getProjectId(), userId);
        CanvasElement element = findElement(canvasId, elementId);

        List<CanvasElement> toDelete = new ArrayList<>();
        toDelete.add(element);
        if (element.getType() != CanvasElementType.ARROW) {

            toDelete.addAll(elementRepository.findConnectors(canvasId, elementId));
        }

        List<UUID> deletedIds = new ArrayList<>();
        for (CanvasElement item : toDelete) {
            deletedIds.add(item.getId());
        }
        elementRepository.deleteAll(toDelete);

        events.publishEvent(CanvasEvent.builder()
                .type(CanvasEventType.ELEMENTS_DELETED)
                .canvasId(canvasId)
                .actorId(userId)
                .elementIds(deletedIds)
                .occurredAt(Instant.now())
                .build());
    }

    @Override
    public ConvertToIssueResponse convertToIssue(UUID canvasId, UUID elementId,
                                                 ConvertToIssueRequest request, UUID userId) {
        Canvas canvas = findCanvas(canvasId);
        Project project = access.project(canvas.getProjectId());
        access.requireMember(project, userId);
        CanvasElement element = findElement(canvasId, elementId);

        if (element.getType() == CanvasElementType.ARROW) {
            throw new BadRequestException("Arrows cannot be converted to issues");
        }
        if (element.getIssueId() != null && issueStillExists(element.getIssueId())) {
            throw new BadRequestException("This item is already linked to an issue");
        }

        String title = "";
        if (request != null && request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            title = request.getTitle().trim();
        } else {
            title = firstLine(element.getText());
        }
        if (title.isEmpty()) {
            throw new BadRequestException("Add some text to this item before converting it to an issue");
        }
        if (title.length() > 200) {
            title = title.substring(0, 200);
        }

        String description = null;
        if (request != null && request.getDescription() != null && !request.getDescription().trim().isEmpty()) {
            description = request.getDescription().trim();
        } else if (element.getText() != null && !element.getText().trim().equals(title)) {
            description = element.getText().trim();
        }
        if (description != null && description.length() > 1000) {
            description = description.substring(0, 1000);
        }

        IssueRequest issueRequest = new IssueRequest();
        issueRequest.setTitle(title);
        issueRequest.setDescription(description);
        if (request != null) {
            issueRequest.setPriority(request.getPriority());
        }
        IssueResponse issue = issueService.create(canvas.getProjectId(), issueRequest, userId);

        element.setIssueId(issue.getId());
        element.setUpdatedBy(userId);
        element.setRevision(element.getRevision() + 1);
        element = elementRepository.save(element);
        publishUpsert(canvasId, userId, element);

        return ConvertToIssueResponse.builder()
                .element(toElementResponse(element))
                .issue(issue)
                .build();
    }

    private void requireEditor(UUID projectId, UUID userId) {
        Project project = access.project(projectId);
        access.require(project, userId, Role.PROJECT_MANAGER, Role.TEAM_LEAD, Role.CONTRIBUTOR);
    }

    private Canvas findCanvas(UUID canvasId) {
        Canvas canvas = canvasRepository.findById(canvasId).orElse(null);
        if (canvas == null) {
            throw new ResourceNotFoundException("Canvas not found");
        }
        return canvas;
    }

    private CanvasElement findElement(UUID canvasId, UUID elementId) {
        CanvasElement element = elementRepository.findByIdAndCanvasId(elementId, canvasId).orElse(null);
        if (element == null) {
            throw new ResourceNotFoundException("Canvas item not found");
        }
        return element;
    }

    private CanvasElement newElement(UUID canvasId, CanvasElementType type, UUID userId) {
        CanvasElement element = new CanvasElement();
        element.setCanvasId(canvasId);
        element.setType(type);
        element.setCreatedBy(userId);
        element.setUpdatedBy(userId);
        return element;
    }

    private UUID checkConnector(UUID canvasId, UUID id) {
        if (id == null) {
            return null;
        }
        CanvasElement target = elementRepository.findByIdAndCanvasId(id, canvasId).orElse(null);
        if (target == null) {
            throw new BadRequestException("Connected item was not found on this canvas");
        }
        if (target.getType() == CanvasElementType.ARROW) {
            throw new BadRequestException("Arrows cannot be attached to other arrows");
        }
        return target.getId();
    }

    private boolean issueStillExists(UUID issueId) {
        Issue issue = issueRepository.findById(issueId).orElse(null);
        return issue != null && issue.getDeletedAt() == null;
    }

    private void publishUpsert(UUID canvasId, UUID actorId, CanvasElement element) {
        List<CanvasElement> one = new ArrayList<>();
        one.add(element);
        events.publishEvent(CanvasEvent.builder()
                .type(CanvasEventType.ELEMENTS_UPSERTED)
                .canvasId(canvasId)
                .actorId(actorId)
                .elements(toElementResponses(one))
                .occurredAt(Instant.now())
                .build());
    }

    private CanvasDetailResponse buildDetail(Canvas canvas, List<CanvasElement> elements) {
        List<Canvas> one = new ArrayList<>();
        one.add(canvas);
        return CanvasDetailResponse.builder()
                .canvas(toCanvasResponses(one).get(0))
                .elements(toElementResponses(elements))
                .build();
    }

    private List<CanvasResponse> toCanvasResponses(List<Canvas> canvases) {
        List<CanvasResponse> result = new ArrayList<>();
        if (canvases.isEmpty()) {
            return result;
        }

        Set<UUID> creatorIds = new HashSet<>();
        for (Canvas canvas : canvases) {
            creatorIds.add(canvas.getCreatedBy());
        }
        Map<UUID, String> names = new HashMap<>();
        for (User user : userRepository.findAllById(creatorIds)) {
            names.put(user.getId(), user.getName());
        }

        for (Canvas canvas : canvases) {
            long count = elementRepository.countByCanvasId(canvas.getId());

            Instant lastActivity = canvas.getUpdatedAt();
            CanvasElement latest = elementRepository.findFirstByCanvasIdOrderByUpdatedAtDesc(canvas.getId());
            if (latest != null && latest.getUpdatedAt() != null) {
                if (lastActivity == null || latest.getUpdatedAt().isAfter(lastActivity)) {
                    lastActivity = latest.getUpdatedAt();
                }
            }

            result.add(CanvasResponse.builder()
                    .id(canvas.getId())
                    .projectId(canvas.getProjectId())
                    .name(canvas.getName())
                    .createdBy(canvas.getCreatedBy())
                    .createdByName(names.get(canvas.getCreatedBy()))
                    .elementCount(count)
                    .lastActivityAt(lastActivity)
                    .createdAt(canvas.getCreatedAt())
                    .updatedAt(canvas.getUpdatedAt())
                    .build());
        }
        return result;
    }

    private CanvasElementResponse toElementResponse(CanvasElement element) {
        List<CanvasElement> one = new ArrayList<>();
        one.add(element);
        return toElementResponses(one).get(0);
    }

    private List<CanvasElementResponse> toElementResponses(List<CanvasElement> elements) {
        // Find the issues that items were converted into (skip deleted issues)
        Set<UUID> issueIds = new HashSet<>();
        for (CanvasElement element : elements) {
            if (element.getIssueId() != null) {
                issueIds.add(element.getIssueId());
            }
        }
        Map<UUID, IssueSummaryResponse> issues = new HashMap<>();
        if (!issueIds.isEmpty()) {
            for (Issue issue : issueRepository.findAllById(issueIds)) {
                if (issue.getDeletedAt() == null) {
                    issues.put(issue.getId(), toSummary(issue));
                }
            }
        }

        List<CanvasElementResponse> result = new ArrayList<>();
        for (CanvasElement e : elements) {
            IssueSummaryResponse linkedIssue = null;
            if (e.getIssueId() != null) {
                linkedIssue = issues.get(e.getIssueId());
            }
            result.add(CanvasElementResponse.builder()
                    .id(e.getId())
                    .canvasId(e.getCanvasId())
                    .type(e.getType())
                    .shape(e.getShape())
                    .x(e.getX())
                    .y(e.getY())
                    .width(e.getWidth())
                    .height(e.getHeight())
                    .endX(e.getEndX())
                    .endY(e.getEndY())
                    .rotation(e.getRotation())
                    .zIndex(e.getZIndex())
                    .text(e.getText())
                    .color(e.getColor())
                    .fromElementId(e.getFromElementId())
                    .toElementId(e.getToElementId())
                    .linkedIssue(linkedIssue)
                    .createdBy(e.getCreatedBy())
                    .updatedBy(e.getUpdatedBy())
                    .revision(e.getRevision())
                    .build());
        }
        return result;
    }

    private IssueSummaryResponse toSummary(Issue i) {
        return IssueSummaryResponse.builder()
                .id(i.getId())
                .issueNumber(i.getIssueNumber())
                .title(i.getTitle())
                .status(i.getStatus())
                .priority(i.getPriority())
                .dueDate(i.getDueDate())
                .build();
    }

    private String defaultColor(CanvasElementType type) {
        if (type == CanvasElementType.STICKY_NOTE) {
            return "#FFF59D";
        }
        if (type == CanvasElementType.SHAPE) {
            return "#E3F2FD";
        }
        if (type == CanvasElementType.ARROW) {
            return "#455A64";
        }
        return "#263238";
    }

    private double valueOrDefault(Double value, double fallback) {
        if (value != null) {
            return value;
        }
        return fallback;
    }

    private String emptyToNull(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        return text;
    }

    private String firstLine(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        int newLine = trimmed.indexOf('\n');
        if (newLine >= 0) {
            trimmed = trimmed.substring(0, newLine);
        }
        return trimmed.trim();
    }
}