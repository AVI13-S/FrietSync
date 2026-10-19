package com.frietsync.backend.repository.budget;

import com.frietsync.backend.entity.budget.ProjectBudget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProjectBudgetRepository extends JpaRepository<ProjectBudget, UUID> {

    Optional<ProjectBudget> findByProjectId(UUID projectId);

    void deleteByProjectId(UUID projectId);
}