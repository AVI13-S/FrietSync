package com.frietsync.backend.controller.budget;

import com.frietsync.backend.dto.budget.BudgetRequest;
import com.frietsync.backend.dto.budget.BudgetResponse;
import com.frietsync.backend.dto.budget.CostReportResponse;
import com.frietsync.backend.dto.budget.ExpenseRequest;
import com.frietsync.backend.dto.budget.ExpenseResponse;
import com.frietsync.backend.dto.budget.SalaryRequest;
import com.frietsync.backend.dto.budget.SalaryResponse;
import com.frietsync.backend.dto.budget.UpdateExpenseRequest;
import com.frietsync.backend.service.budget.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @PutMapping("/projects/{projectId}/budget")
    public ResponseEntity<BudgetResponse> setBudget(@PathVariable UUID projectId,
                                                    @Valid @RequestBody BudgetRequest request,
                                                    @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(budgetService.setBudget(projectId, request, userId));
    }

    @GetMapping("/projects/{projectId}/budget")
    public ResponseEntity<BudgetResponse> getBudget(@PathVariable UUID projectId,
                                                    @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(budgetService.getBudget(projectId, userId));
    }

    @GetMapping("/projects/{projectId}/budget/report")
    public ResponseEntity<CostReportResponse> report(@PathVariable UUID projectId,
                                                     @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(budgetService.report(projectId, userId));
    }

    @PostMapping("/projects/{projectId}/expenses")
    public ResponseEntity<ExpenseResponse> addExpense(@PathVariable UUID projectId,
                                                      @Valid @RequestBody ExpenseRequest request,
                                                      @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(budgetService.addExpense(projectId, request, userId));
    }

    @GetMapping("/projects/{projectId}/expenses")
    public ResponseEntity<List<ExpenseResponse>> listExpenses(@PathVariable UUID projectId,
                                                              @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(budgetService.listExpenses(projectId, userId));
    }

    @PatchMapping("/expenses/{expenseId}")
    public ResponseEntity<ExpenseResponse> updateExpense(@PathVariable UUID expenseId,
                                                         @Valid @RequestBody UpdateExpenseRequest request,
                                                         @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(budgetService.updateExpense(expenseId, request, userId));
    }

    @DeleteMapping("/expenses/{expenseId}")
    public ResponseEntity<Void> deleteExpense(@PathVariable UUID expenseId,
                                              @AuthenticationPrincipal UUID userId) {
        budgetService.deleteExpense(expenseId, userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/users/{userId}/salary")
    public ResponseEntity<SalaryResponse> setSalary(@PathVariable UUID userId,
                                                    @Valid @RequestBody SalaryRequest request,
                                                    @AuthenticationPrincipal UUID adminId) {
        return ResponseEntity.ok(budgetService.setSalary(userId, request, adminId));
    }
}