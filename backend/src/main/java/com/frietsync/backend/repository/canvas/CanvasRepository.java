package com.frietsync.backend.repository.canvas;

import com.frietsync.backend.entity.canvas.Canvas;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CanvasRepository extends JpaRepository<Canvas, UUID> {

    List<Canvas> findByProjectIdOrderByUpdatedAtDesc(UUID projectId);

    List<Canvas> findByProjectId(UUID projectId);

    long countByProjectId(UUID projectId);
}