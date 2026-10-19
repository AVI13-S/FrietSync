package com.frietsync.backend.repository.budget;

import com.frietsync.backend.entity.budget.ProjectExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectExpenseRepository extends JpaRepository<ProjectExpense, UUID> {

    List<ProjectExpense> findByProjectIdOrderByExpenseDateDescCreatedAtDesc(UUID projectId);

    void deleteByProjectId(UUID projectId);
}