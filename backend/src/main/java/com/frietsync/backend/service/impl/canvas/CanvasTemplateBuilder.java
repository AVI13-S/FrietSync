package com.frietsync.backend.service.impl.canvas;

import com.frietsync.backend.entity.canvas.CanvasElement;
import com.frietsync.backend.entity.canvas.CanvasElementType;
import com.frietsync.backend.entity.canvas.CanvasShape;
import com.frietsync.backend.entity.canvas.CanvasTemplate;
import com.frietsync.backend.repository.canvas.CanvasElementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CanvasTemplateBuilder {

    private final CanvasElementRepository elementRepository;

    public List<CanvasElement> build(UUID canvasId, CanvasTemplate template, UUID userId) {
        List<CanvasElement> created = new ArrayList<>();
        if (template == CanvasTemplate.FLOWCHART) {
            buildFlowchart(created, canvasId, userId);
        } else if (template == CanvasTemplate.ARCHITECTURE) {
            buildArchitecture(created, canvasId, userId);
        } else if (template == CanvasTemplate.USER_FLOW) {
            buildUserFlow(created, canvasId, userId);
        }
        return created;
    }

    private void buildFlowchart(List<CanvasElement> created, UUID canvasId, UUID userId) {
        CanvasElement start = addShape(created, canvasId, userId, CanvasShape.ROUNDED_RECTANGLE,
                300, 0, 160, 60, "Start", "#C8E6C9");
        CanvasElement process = addShape(created, canvasId, userId, CanvasShape.RECTANGLE,
                300, 120, 160, 70, "Process step", "#E3F2FD");
        CanvasElement decision = addShape(created, canvasId, userId, CanvasShape.DIAMOND,
                280, 250, 200, 120, "Condition?", "#FFF9C4");
        CanvasElement yes = addShape(created, canvasId, userId, CanvasShape.RECTANGLE,
                560, 275, 160, 70, "Yes: do this", "#E3F2FD");
        CanvasElement no = addShape(created, canvasId, userId, CanvasShape.RECTANGLE,
                40, 275, 160, 70, "No: do that", "#E3F2FD");
        CanvasElement end = addShape(created, canvasId, userId, CanvasShape.ROUNDED_RECTANGLE,
                300, 450, 160, 60, "End", "#FFCDD2");

        addArrow(created, canvasId, userId, start, process, null);
        addArrow(created, canvasId, userId, process, decision, null);
        addArrow(created, canvasId, userId, decision, yes, "Yes");
        addArrow(created, canvasId, userId, decision, no, "No");
        addArrow(created, canvasId, userId, yes, end, null);
        addArrow(created, canvasId, userId, no, end, null);
    }

    private void buildArchitecture(List<CanvasElement> created, UUID canvasId, UUID userId) {
        addText(created, canvasId, userId, 0, -60, 360, 40, "Architecture diagram");
        CanvasElement client = addShape(created, canvasId, userId, CanvasShape.ROUNDED_RECTANGLE,
                0, 120, 160, 70, "Client (web / mobile)", "#E1F5FE");
        CanvasElement gateway = addShape(created, canvasId, userId, CanvasShape.RECTANGLE,
                260, 120, 160, 70, "API gateway", "#FFF3E0");
        CanvasElement auth = addShape(created, canvasId, userId, CanvasShape.RECTANGLE,
                520, 0, 160, 70, "Auth service", "#E8F5E9");
        CanvasElement core = addShape(created, canvasId, userId, CanvasShape.RECTANGLE,
                520, 120, 160, 70, "Core service", "#E8F5E9");
        CanvasElement notify = addShape(created, canvasId, userId, CanvasShape.RECTANGLE,
                520, 240, 160, 70, "Notification service", "#E8F5E9");
        CanvasElement database = addShape(created, canvasId, userId, CanvasShape.CYLINDER,
                800, 100, 120, 110, "Database", "#F3E5F5");

        addArrow(created, canvasId, userId, client, gateway, null);
        addArrow(created, canvasId, userId, gateway, auth, null);
        addArrow(created, canvasId, userId, gateway, core, null);
        addArrow(created, canvasId, userId, gateway, notify, null);
        addArrow(created, canvasId, userId, auth, database, null);
        addArrow(created, canvasId, userId, core, database, null);
    }

    private void buildUserFlow(List<CanvasElement> created, UUID canvasId, UUID userId) {
        CanvasElement landing = addShape(created, canvasId, userId, CanvasShape.ROUNDED_RECTANGLE,
                0, 100, 160, 70, "Landing page", "#E1F5FE");
        CanvasElement signup = addShape(created, canvasId, userId, CanvasShape.RECTANGLE,
                240, 100, 160, 70, "Sign up", "#E3F2FD");
        CanvasElement verify = addShape(created, canvasId, userId, CanvasShape.RECTANGLE,
                480, 100, 160, 70, "Verify email", "#E3F2FD");
        CanvasElement onboarding = addShape(created, canvasId, userId, CanvasShape.RECTANGLE,
                720, 100, 160, 70, "Onboarding", "#E3F2FD");
        CanvasElement dashboard = addShape(created, canvasId, userId, CanvasShape.ROUNDED_RECTANGLE,
                960, 100, 160, 70, "Dashboard", "#C8E6C9");
        addNote(created, canvasId, userId, 240, 260, 220, 160,
                "Notes: add pain points and open questions here.");

        addArrow(created, canvasId, userId, landing, signup, null);
        addArrow(created, canvasId, userId, signup, verify, null);
        addArrow(created, canvasId, userId, verify, onboarding, null);
        addArrow(created, canvasId, userId, onboarding, dashboard, null);
    }

    private CanvasElement addShape(List<CanvasElement> created, UUID canvasId, UUID userId,
                                   CanvasShape shape, double x, double y, double width, double height,
                                   String text, String color) {
        CanvasElement element = newElement(created, canvasId, userId, CanvasElementType.SHAPE);
        element.setShape(shape);
        element.setX(x);
        element.setY(y);
        element.setWidth(width);
        element.setHeight(height);
        element.setText(text);
        element.setColor(color);
        return save(created, element);
    }

    private CanvasElement addNote(List<CanvasElement> created, UUID canvasId, UUID userId,
                                  double x, double y, double width, double height, String text) {
        CanvasElement element = newElement(created, canvasId, userId, CanvasElementType.STICKY_NOTE);
        element.setX(x);
        element.setY(y);
        element.setWidth(width);
        element.setHeight(height);
        element.setText(text);
        element.setColor("#FFF59D");
        return save(created, element);
    }

    private CanvasElement addText(List<CanvasElement> created, UUID canvasId, UUID userId,
                                  double x, double y, double width, double height, String text) {
        CanvasElement element = newElement(created, canvasId, userId, CanvasElementType.TEXT);
        element.setX(x);
        element.setY(y);
        element.setWidth(width);
        element.setHeight(height);
        element.setText(text);
        element.setColor("#263238");
        return save(created, element);
    }

    private CanvasElement addArrow(List<CanvasElement> created, UUID canvasId, UUID userId,
                                   CanvasElement from, CanvasElement to, String label) {
        double fromCenterX = from.getX() + from.getWidth() / 2;
        double fromCenterY = from.getY() + from.getHeight() / 2;
        double toCenterX = to.getX() + to.getWidth() / 2;
        double toCenterY = to.getY() + to.getHeight() / 2;
        double diffX = toCenterX - fromCenterX;
        double diffY = toCenterY - fromCenterY;

        double startX;
        double startY;
        double endX;
        double endY;
        if (Math.abs(diffX) >= Math.abs(diffY)) {
            startY = fromCenterY;
            endY = toCenterY;
            if (diffX >= 0) {
                startX = from.getX() + from.getWidth();
                endX = to.getX();
            } else {
                startX = from.getX();
                endX = to.getX() + to.getWidth();
            }
        } else {
            startX = fromCenterX;
            endX = toCenterX;
            if (diffY >= 0) {
                startY = from.getY() + from.getHeight();
                endY = to.getY();
            } else {
                startY = from.getY();
                endY = to.getY() + to.getHeight();
            }
        }

        CanvasElement arrow = newElement(created, canvasId, userId, CanvasElementType.ARROW);
        arrow.setX(startX);
        arrow.setY(startY);
        arrow.setEndX(endX);
        arrow.setEndY(endY);
        arrow.setFromElementId(from.getId());
        arrow.setToElementId(to.getId());
        arrow.setText(label);
        arrow.setColor("#455A64");
        return save(created, arrow);
    }

    private CanvasElement newElement(List<CanvasElement> created, UUID canvasId, UUID userId,
                                     CanvasElementType type) {
        CanvasElement element = new CanvasElement();
        element.setCanvasId(canvasId);
        element.setType(type);
        element.setCreatedBy(userId);
        element.setUpdatedBy(userId);
        element.setZIndex(created.size());
        return element;
    }

    private CanvasElement save(List<CanvasElement> created, CanvasElement element) {
        CanvasElement saved = elementRepository.save(element);
        created.add(saved);
        return saved;
    }
}