package com.frietsync.backend.service.impl.canvas;

import com.frietsync.backend.dto.canvas.CanvasDetailResponse;
import com.frietsync.backend.dto.canvas.CanvasElementRequest;
import com.frietsync.backend.dto.canvas.CanvasElementResponse;
import com.frietsync.backend.dto.canvas.CanvasElementUpdateRequest;
import com.frietsync.backend.dto.canvas.CanvasEvent;
import com.frietsync.backend.dto.canvas.CanvasEventType;
import com.frietsync.backend.dto.canvas.CanvasResponse;
import com.frietsync.backend.dto.canvas.ConvertToIssueRequest;
import com.frietsync.backend.dto.canvas.ConvertToIssueResponse;
import com.frietsync.backend.dto.canvas.CreateCanvasRequest;
import com.frietsync.backend.dto.canvas.RenameCanvasRequest;
import com.frietsync.backend.dto.issue.IssueRequest;
import com.frietsync.backend.dto.issue.IssueResponse;
import com.frietsync.backend.dto.issue.IssueSummaryResponse;
import com.frietsync.backend.entity.canvas.Canvas;
import com.frietsync.backend.entity.canvas.CanvasElement;
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
    private final ApplicationEventPublisher events;

    @Override
    public CanvasDetailResponse createCanvas(UUID projectId, CreateCanvasRequest request, UUID userId) {
        requireEditor(projectId, userId);
        String name = cleanName(request.getName());
        if (canvasRepository.countByProjectId(projectId) >= MAX_CANVASES_PER_PROJECT) {
            throw new BadRequestException("A project can have at most " + MAX_CANVASES_PER_PROJECT + " canvases");
        }

        Canvas canvas = new Canvas();
        canvas.setProjectId(projectId);
        canvas.setName(name);
        canvas.setCreatedBy(userId);
        canvas = canvasRepository.save(canvas);

        return buildDetail(canvas, new ArrayList<CanvasElement>());
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
        canvas.setName(cleanName(request.getName()));
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

        if (r.getType() == null) {
            throw new BadRequestException("Item type is required");
        }
        if (r.getX() == null || r.getY() == null) {
            throw new BadRequestException("Item position (x and y) is required");
        }
        checkFieldLengths(r.getText(), r.getColor(), r.getShape());
        if (elementRepository.countByCanvasId(canvasId) >= MAX_ELEMENTS_PER_CANVAS) {
            throw new BadRequestException("A canvas can have at most " + MAX_ELEMENTS_PER_CANVAS + " items");
        }

        CanvasElement element = new CanvasElement();
        element.setCanvasId(canvasId);
        element.setType(r.getType());
        element.setShape(r.getShape());
        element.setX(r.getX());
        element.setY(r.getY());
        element.setWidth(r.getWidth());
        element.setHeight(r.getHeight());
        element.setEndX(r.getEndX());
        element.setEndY(r.getEndY());
        element.setFromElementId(r.getFromElementId());
        element.setToElementId(r.getToElementId());
        if (r.getRotation() != null) {
            element.setRotation(r.getRotation());
        }
        if (r.getZIndex() != null) {
            element.setZIndex(r.getZIndex());
        }
        element.setText(r.getText());
        element.setColor(r.getColor());
        element.setCreatedBy(userId);
        element.setUpdatedBy(userId);

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
        checkFieldLengths(r.getText(), r.getColor(), r.getShape());

        if (r.getShape() != null) {
            element.setShape(r.getShape());
        }
        if (r.getX() != null) {
            element.setX(r.getX());
        }
        if (r.getY() != null) {
            element.setY(r.getY());
        }
        if (r.getWidth() != null) {
            element.setWidth(r.getWidth());
        }
        if (r.getHeight() != null) {
            element.setHeight(r.getHeight());
        }
        if (r.getEndX() != null) {
            element.setEndX(r.getEndX());
        }
        if (r.getEndY() != null) {
            element.setEndY(r.getEndY());
        }
        if (r.getFromElementId() != null) {
            element.setFromElementId(r.getFromElementId());
        }
        if (r.getToElementId() != null) {
            element.setToElementId(r.getToElementId());
        }
        if (r.getRotation() != null) {
            element.setRotation(r.getRotation());
        }
        if (r.getZIndex() != null) {
            element.setZIndex(r.getZIndex());
        }
        if (r.getText() != null) {
            element.setText(r.getText());
        }
        if (r.getColor() != null) {
            element.setColor(r.getColor());
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
        for (CanvasElement arrow : elementRepository.findConnectors(canvasId, elementId)) {
            if (!arrow.getId().equals(elementId)) {
                toDelete.add(arrow);
            }
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

        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new BadRequestException("Issue title is required");
        }
        String title = request.getTitle().trim();
        if (title.length() > 200) {
            throw new BadRequestException("Issue title must be at most 200 characters");
        }
        if (request.getDescription() != null && request.getDescription().length() > 1000) {
            throw new BadRequestException("Issue description must be at most 1000 characters");
        }
        if (element.getIssueId() != null && issueStillExists(element.getIssueId())) {
            throw new BadRequestException("This item is already linked to an issue");
        }

        IssueRequest issueRequest = new IssueRequest();
        issueRequest.setTitle(title);
        issueRequest.setDescription(request.getDescription());
        issueRequest.setPriority(request.getPriority());
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

    private String cleanName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BadRequestException("Canvas name is required");
        }
        String trimmed = name.trim();
        if (trimmed.length() > 200) {
            throw new BadRequestException("Canvas name must be at most 200 characters");
        }
        return trimmed;
    }

    private void checkFieldLengths(String text, String color, String shape) {
        if (text != null && text.length() > 2000) {
            throw new BadRequestException("Text must be at most 2000 characters");
        }
        if (color != null && color.length() > 7) {
            throw new BadRequestException("Color must be at most 7 characters, like #FFF59D");
        }
        if (shape != null && shape.length() > 30) {
            throw new BadRequestException("Shape name must be at most 30 characters");
        }
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
}