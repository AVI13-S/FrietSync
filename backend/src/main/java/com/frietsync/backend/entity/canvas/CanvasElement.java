package com.frietsync.backend.entity.canvas;

import com.frietsync.backend.entity.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.DynamicUpdate;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@DynamicUpdate
@Table(name = "canvas_elements",
        indexes = @Index(name = "idx_canvas_elements_canvas", columnList = "canvas_id"))
public class CanvasElement extends BaseEntity {

    @Column(name = "canvas_id", nullable = false, updatable = false)
    private UUID canvasId;

    @Enumerated(EnumType.STRING)
    @Column(name = "element_type", nullable = false, updatable = false, length = 20)
    private CanvasElementType type;

    @Column(name = "shape_type", length = 30)
    private String shape;

    @Column(name = "pos_x", nullable = false)
    private double x;

    @Column(name = "pos_y", nullable = false)
    private double y;

    @Column(name = "width")
    private Double width;

    @Column(name = "height")
    private Double height;

    @Column(name = "end_x")
    private Double endX;

    @Column(name = "end_y")
    private Double endY;

    @Column(nullable = false)
    private double rotation = 0;

    @Column(name = "z_index", nullable = false)
    private int zIndex = 0;

    @Column(length = 2000)
    private String text;

    @Column(length = 10)
    private String color;

    @Column(name = "from_element_id")
    private UUID fromElementId;

    @Column(name = "to_element_id")
    private UUID toElementId;

    @Column(name = "issue_id")
    private UUID issueId;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(nullable = false)
    private long revision = 0;
}