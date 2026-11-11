package com.frietsync.backend.dto.canvas;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
public class CanvasElementUpdateRequest {

    private Long revision;
    private String shape;
    private Double x;
    private Double y;
    private Double width;
    private Double height;
    private Double endX;
    private Double endY;
    private UUID fromElementId;
    private UUID toElementId;
    private Double rotation;
    private Integer zIndex;
    private String text;
    private String color;

    @JsonIgnore
    private final Set<String> providedFields = new HashSet<>();

    @JsonSetter("shape")
    public void setShape(String shape) {
        this.shape = shape;
        providedFields.add("shape");
    }

    @JsonSetter("x")
    public void setX(Double x) {
        this.x = x;
        providedFields.add("x");
    }

    @JsonSetter("y")
    public void setY(Double y) {
        this.y = y;
        providedFields.add("y");
    }

    @JsonSetter("width")
    public void setWidth(Double width) {
        this.width = width;
        providedFields.add("width");
    }

    @JsonSetter("height")
    public void setHeight(Double height) {
        this.height = height;
        providedFields.add("height");
    }

    @JsonSetter("endX")
    public void setEndX(Double endX) {
        this.endX = endX;
        providedFields.add("endX");
    }

    @JsonSetter("endY")
    public void setEndY(Double endY) {
        this.endY = endY;
        providedFields.add("endY");
    }

    @JsonSetter("fromElementId")
    public void setFromElementId(UUID fromElementId) {
        this.fromElementId = fromElementId;
        providedFields.add("fromElementId");
    }

    @JsonSetter("toElementId")
    public void setToElementId(UUID toElementId) {
        this.toElementId = toElementId;
        providedFields.add("toElementId");
    }

    @JsonSetter("rotation")
    public void setRotation(Double rotation) {
        this.rotation = rotation;
        providedFields.add("rotation");
    }

    @JsonSetter("zIndex")
    public void setZIndex(Integer zIndex) {
        this.zIndex = zIndex;
        providedFields.add("zIndex");
    }

    @JsonSetter("text")
    public void setText(String text) {
        this.text = text;
        providedFields.add("text");
    }

    @JsonSetter("color")
    public void setColor(String color) {
        this.color = color;
        providedFields.add("color");
    }

    @JsonIgnore
    public boolean isProvided(String fieldName) {
        return providedFields.contains(fieldName);
    }
}