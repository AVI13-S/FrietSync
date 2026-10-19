package com.frietsync.backend.service.budget;

import com.frietsync.backend.dto.budget.BudgetRequest;
import com.frietsync.backend.dto.budget.BudgetResponse;
import com.frietsync.backend.dto.budget.CostReportResponse;
import com.frietsync.backend.dto.budget.ExpenseRequest;
import com.frietsync.backend.dto.budget.ExpenseResponse;
import com.frietsync.backend.dto.budget.SalaryRequest;
import com.frietsync.backend.dto.budget.SalaryResponse;
import com.frietsync.backend.dto.budget.UpdateExpenseRequest;

import java.util.List;
import java.util.UUID;

public interface BudgetService {

    BudgetResponse setBudget(UUID projectId, BudgetRequest request, UUID userId);

    BudgetResponse getBudget(UUID projectId, UUID userId);

    CostReportResponse report(UUID projectId, UUID userId);

    ExpenseResponse addExpense(UUID projectId, ExpenseRequest request, UUID userId);

    List<ExpenseResponse> listExpenses(UUID projectId, UUID userId);

    ExpenseResponse updateExpense(UUID expenseId, UpdateExpenseRequest request, UUID userId);

    void deleteExpense(UUID expenseId, UUID userId);

    SalaryResponse setSalary(UUID targetUserId, SalaryRequest request, UUID adminId);
}