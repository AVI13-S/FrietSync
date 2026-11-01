package com.frietsync.backend.repository.canvas;

import com.frietsync.backend.entity.canvas.CanvasElement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CanvasElementRepository extends JpaRepository<CanvasElement, UUID> {

    List<CanvasElement> findByCanvasIdOrderByZIndexAscCreatedAtAsc(UUID canvasId);

    Optional<CanvasElement> findByIdAndCanvasId(UUID id, UUID canvasId);

    long countByCanvasId(UUID canvasId);

    void deleteByCanvasId(UUID canvasId);

    @Query("select e from CanvasElement e where e.canvasId = :canvasId "
            + "and (e.fromElementId = :elementId or e.toElementId = :elementId)")
    List<CanvasElement> findConnectors(@Param("canvasId") UUID canvasId,
                                       @Param("elementId") UUID elementId);

    @Query("select coalesce(max(e.zIndex), 0) from CanvasElement e where e.canvasId = :canvasId")
    Integer findMaxZIndex(@Param("canvasId") UUID canvasId);

    @Query("select e.canvasId, count(e), max(e.updatedAt) from CanvasElement e "
            + "where e.canvasId in :canvasIds group by e.canvasId")
    List<Object[]> findStats(@Param("canvasIds") Collection<UUID> canvasIds);
}