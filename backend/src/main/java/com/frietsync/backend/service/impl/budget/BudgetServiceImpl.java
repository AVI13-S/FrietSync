package com.frietsync.backend.service.impl.budget;

import com.frietsync.backend.dto.budget.BudgetRequest;
import com.frietsync.backend.dto.budget.BudgetResponse;
import com.frietsync.backend.dto.budget.CostLineResponse;
import com.frietsync.backend.dto.budget.CostReportResponse;
import com.frietsync.backend.dto.budget.ExpenseRequest;
import com.frietsync.backend.dto.budget.ExpenseResponse;
import com.frietsync.backend.dto.budget.MemberCostResponse;
import com.frietsync.backend.dto.budget.SalaryRequest;
import com.frietsync.backend.dto.budget.SalaryResponse;
import com.frietsync.backend.dto.budget.SprintCostResponse;
import com.frietsync.backend.dto.budget.UpdateExpenseRequest;
import com.frietsync.backend.entity.budget.BudgetStatus;
import com.frietsync.backend.entity.budget.ExpenseCategory;
import com.frietsync.backend.entity.budget.ProjectBudget;
import com.frietsync.backend.entity.budget.ProjectExpense;
import com.frietsync.backend.entity.budget.UserSalary;
import com.frietsync.backend.entity.issue.Issue;
import com.frietsync.backend.entity.issue.IssueStatus;
import com.frietsync.backend.entity.project.Project;
import com.frietsync.backend.entity.project.ProjectMember;
import com.frietsync.backend.entity.project.ProjectStatus;
import com.frietsync.backend.entity.sprint.Sprint;
import com.frietsync.backend.entity.user.Role;
import com.frietsync.backend.entity.user.User;
import com.frietsync.backend.exception.BadRequestException;
import com.frietsync.backend.exception.ForbiddenException;
import com.frietsync.backend.exception.ResourceNotFoundException;
import com.frietsync.backend.repository.budget.ProjectBudgetRepository;
import com.frietsync.backend.repository.budget.ProjectExpenseRepository;
import com.frietsync.backend.repository.budget.UserSalaryRepository;
import com.frietsync.backend.repository.issue.IssueRepository;
import com.frietsync.backend.repository.project.ProjectMemberRepository;
import com.frietsync.backend.repository.sprint.SprintRepository;
import com.frietsync.backend.repository.user.UserRepository;
import com.frietsync.backend.service.budget.BudgetService;
import com.frietsync.backend.service.impl.project.ProjectAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal DAYS_PER_MONTH = BigDecimal.valueOf(30);

    private final ProjectAccess projectAccess;
    private final ProjectBudgetRepository budgetRepository;
    private final ProjectExpenseRepository expenseRepository;
    private final UserSalaryRepository salaryRepository;
    private final ProjectMemberRepository memberRepository;
    private final IssueRepository issueRepository;
    private final SprintRepository sprintRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public BudgetResponse setBudget(UUID projectId, BudgetRequest request, UUID userId) {
        Project project = projectAccess.project(projectId);
        projectAccess.require(project, userId, Role.PROJECT_MANAGER);

        ProjectBudget budget = budgetRepository.findByProjectId(projectId).orElse(null);
        if (budget == null) {
            budget = new ProjectBudget();
            budget.setProjectId(projectId);
        }
        budget.setTotalBudget(request.getTotalBudget());
        budget = budgetRepository.save(budget);

        return buildBudget(project, budget);
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetResponse getBudget(UUID projectId, UUID userId) {
        Project project = projectAccess.project(projectId);
        projectAccess.require(project, userId, Role.PROJECT_MANAGER);

        ProjectBudget budget = budgetRepository.findByProjectId(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget is not set for this project"));
        return buildBudget(project, budget);
    }

    private BudgetResponse buildBudget(Project project, ProjectBudget budget) {
        Instant now = Instant.now();
        Instant end = costEnd(project, now);

        Map<UUID, Instant> starts = startDates(project, memberRepository.findByProjectId(project.getId()));
        Map<UUID, BigDecimal> salaries = salariesOf(starts.keySet());
        BigDecimal salaryCost = BigDecimal.ZERO;
        int withoutSalary = 0;
        for (UUID personId : starts.keySet()) {
            BigDecimal monthly = salaries.get(personId);
            if (monthly == null) {
                withoutSalary++;
            } else {
                salaryCost = salaryCost.add(salaryFor(monthly, starts.get(personId), end));
            }
        }

        BigDecimal expenseCost = BigDecimal.ZERO;
        for (ProjectExpense expense
                : expenseRepository.findByProjectIdOrderByExpenseDateDescCreatedAtDesc(project.getId())) {
            expenseCost = expenseCost.add(expense.getAmount());
        }

        BigDecimal total = budget.getTotalBudget();
        BigDecimal actual = salaryCost.add(expenseCost);
        BigDecimal remaining = total.subtract(actual);
        BigDecimal percentUsed = actual.multiply(HUNDRED).divide(total, 1, RoundingMode.HALF_UP);

        long daysElapsed = daysBetween(project.getCreatedAt(), end);
        if (daysElapsed < 1) {
            daysElapsed = 1;
        }
        BigDecimal dailyBurn = actual.divide(BigDecimal.valueOf(daysElapsed), 2, RoundingMode.HALF_UP);
        int totalIssues = 0;
        int finishedIssues = 0;
        for (Issue issue : issueRepository.findByProjectIdAndDeletedAtIsNull(project.getId())) {
            totalIssues++;
            if (issue.getStatus() == IssueStatus.RESOLVED || issue.getStatus() == IssueStatus.CLOSED) {
                finishedIssues++;
            }
        }
        int remainingIssues = totalIssues - finishedIssues;

        Long remainingDays = null;
        LocalDate today = now.atZone(ZoneOffset.UTC).toLocalDate();
        if (project.getStatus() == ProjectStatus.COMPLETED || project.getStatus() == ProjectStatus.CANCELLED) {
            remainingDays = 0L;
        } else if (totalIssues > 0 && remainingIssues == 0) {
            remainingDays = 0L;
        } else if (finishedIssues > 0) {
            remainingDays = (long) remainingIssues * daysElapsed / finishedIssues;
        } else if (project.getDeadline() != null && project.getDeadline().isAfter(today)) {
            remainingDays = ChronoUnit.DAYS.between(today, project.getDeadline());
        }

        BigDecimal projected = null;
        if (remainingDays != null) {
            projected = actual.add(dailyBurn.multiply(BigDecimal.valueOf(remainingDays)));
        }

        BudgetStatus status = BudgetStatus.ON_TRACK;
        String message = "Spending is within the budget.";
        if (actual.compareTo(total) > 0) {
            status = BudgetStatus.EXCEEDED;
            message = "The budget is already exceeded by " + actual.subtract(total).toPlainString() + ".";
        } else if (projected != null && projected.compareTo(total) > 0) {
            status = BudgetStatus.AT_RISK;
            message = "The projected final cost " + projected.toPlainString() + " is above the budget "
                    + total.toPlainString() + " by " + projected.subtract(total).toPlainString() + ".";
        } else if (projected == null) {
            message = "Not enough data to project the final cost yet.";
        }

        return BudgetResponse.builder()
                .projectId(project.getId())
                .totalBudget(total)
                .salaryCost(salaryCost)
                .expenseCost(expenseCost)
                .actualCost(actual)
                .remainingBudget(remaining)
                .percentUsed(percentUsed)
                .dailyBurnRate(dailyBurn)
                .remainingIssues(remainingIssues)
                .estimatedRemainingDays(remainingDays)
                .projectedFinalCost(projected)
                .status(status)
                .alertMessage(message)
                .membersWithoutSalary(withoutSalary)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CostReportResponse report(UUID projectId, UUID userId) {
        Project project = projectAccess.project(projectId);
        projectAccess.require(project, userId, Role.PROJECT_MANAGER);

        Instant end = costEnd(project, Instant.now());
        Map<UUID, Instant> starts = startDates(project, memberRepository.findByProjectId(projectId));
        Map<UUID, BigDecimal> salaries = salariesOf(starts.keySet());
        Map<UUID, String> names = userNames(starts.keySet());

        List<MemberCostResponse> byMember = new ArrayList<>();
        BigDecimal salaryTotal = BigDecimal.ZERO;
        for (UUID personId : starts.keySet()) {
            BigDecimal monthly = salaries.get(personId);
            if (monthly == null) {
                continue;
            }
            BigDecimal cost = salaryFor(monthly, starts.get(personId), end);
            salaryTotal = salaryTotal.add(cost);
            byMember.add(MemberCostResponse.builder()
                    .userId(personId)
                    .name(names.get(personId))
                    .role(projectAccess.roleOf(project, personId))
                    .monthlySalary(monthly)
                    .daysOnProject(daysBetween(starts.get(personId), end))
                    .salaryCost(cost)
                    .build());
        }

        Map<ExpenseCategory, BigDecimal> expenseByCategory = new HashMap<>();
        Map<UUID, BigDecimal> expenseBySprint = new HashMap<>();
        BigDecimal expenseTotal = BigDecimal.ZERO;
        for (ProjectExpense expense
                : expenseRepository.findByProjectIdOrderByExpenseDateDescCreatedAtDesc(projectId)) {
            expenseTotal = expenseTotal.add(expense.getAmount());

            BigDecimal categoryAmount = expenseByCategory.get(expense.getCategory());
            if (categoryAmount == null) {
                categoryAmount = BigDecimal.ZERO;
            }
            expenseByCategory.put(expense.getCategory(), categoryAmount.add(expense.getAmount()));

            if (expense.getSprintId() != null) {
                BigDecimal sprintAmount = expenseBySprint.get(expense.getSprintId());
                if (sprintAmount == null) {
                    sprintAmount = BigDecimal.ZERO;
                }
                expenseBySprint.put(expense.getSprintId(), sprintAmount.add(expense.getAmount()));
            }
        }

        List<CostLineResponse> byCategory = new ArrayList<>();
        byCategory.add(CostLineResponse.builder().category("SALARY").amount(salaryTotal).build());
        for (ExpenseCategory category : ExpenseCategory.values()) {
            BigDecimal amount = expenseByCategory.get(category);
            if (amount != null) {
                byCategory.add(CostLineResponse.builder().category(category.name()).amount(amount).build());
            }
        }

        List<SprintCostResponse> bySprint = new ArrayList<>();
        for (Sprint sprint : sprintRepository.findByProjectId(projectId)) {
            BigDecimal sprintSalary = BigDecimal.ZERO;
            if (sprint.getStartDate() != null) {
                Instant sprintStart = startOfDay(sprint.getStartDate());
                Instant sprintEnd = end;
                if (sprint.getEndDate() != null) {
                    Instant endOfSprintDay = startOfDay(sprint.getEndDate().plusDays(1));
                    if (endOfSprintDay.isBefore(end)) {
                        sprintEnd = endOfSprintDay;
                    }
                }
                for (UUID personId : starts.keySet()) {
                    BigDecimal monthly = salaries.get(personId);
                    if (monthly == null) {
                        continue;
                    }
                    Instant from = starts.get(personId);
                    if (sprintStart.isAfter(from)) {
                        from = sprintStart;
                    }
                    sprintSalary = sprintSalary.add(salaryFor(monthly, from, sprintEnd));
                }
            }
            BigDecimal sprintExpense = expenseBySprint.get(sprint.getId());
            if (sprintExpense == null) {
                sprintExpense = BigDecimal.ZERO;
            }
            bySprint.add(SprintCostResponse.builder()
                    .sprintId(sprint.getId())
                    .sprintNumber(sprint.getSprintNumber())
                    .name(sprint.getName())
                    .salaryCost(sprintSalary)
                    .expenseCost(sprintExpense)
                    .totalCost(sprintSalary.add(sprintExpense))
                    .build());
        }

        return CostReportResponse.builder()
                .projectId(projectId)
                .totalCost(salaryTotal.add(expenseTotal))
                .byCategory(byCategory)
                .byMember(byMember)
                .bySprint(bySprint)
                .build();
    }

    @Override
    @Transactional
    public ExpenseResponse addExpense(UUID projectId, ExpenseRequest request, UUID userId) {
        Project project = projectAccess.project(projectId);
        projectAccess.require(project, userId, Role.PROJECT_MANAGER);
        checkSprint(projectId, request.getSprintId());

        ProjectExpense expense = new ProjectExpense();
        expense.setProjectId(projectId);
        expense.setCategory(request.getCategory());
        expense.setTitle(request.getTitle().trim());
        expense.setAmount(request.getAmount());
        if (request.getExpenseDate() == null) {
            expense.setExpenseDate(LocalDate.now(ZoneOffset.UTC));
        } else {
            expense.setExpenseDate(request.getExpenseDate());
        }
        expense.setSprintId(request.getSprintId());
        expense.setCreatedBy(userId);
        expense = expenseRepository.save(expense);

        return toResponse(expense);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseResponse> listExpenses(UUID projectId, UUID userId) {
        Project project = projectAccess.project(projectId);
        projectAccess.require(project, userId, Role.PROJECT_MANAGER);

        List<ExpenseResponse> result = new ArrayList<>();
        for (ProjectExpense expense
                : expenseRepository.findByProjectIdOrderByExpenseDateDescCreatedAtDesc(projectId)) {
            result.add(toResponse(expense));
        }
        return result;
    }

    @Override
    @Transactional
    public ExpenseResponse updateExpense(UUID expenseId, UpdateExpenseRequest request, UUID userId) {
        ProjectExpense expense = findExpense(expenseId);
        Project project = projectAccess.project(expense.getProjectId());
        projectAccess.require(project, userId, Role.PROJECT_MANAGER);

        if (request.getCategory() == null && request.getTitle() == null && request.getAmount() == null
                && request.getExpenseDate() == null && request.getSprintId() == null) {
            throw new BadRequestException("Nothing to update");
        }
        if (request.getCategory() != null) {
            expense.setCategory(request.getCategory());
        }
        if (request.getTitle() != null) {
            if (request.getTitle().isBlank()) {
                throw new BadRequestException("Title must not be blank");
            }
            expense.setTitle(request.getTitle().trim());
        }
        if (request.getAmount() != null) {
            expense.setAmount(request.getAmount());
        }
        if (request.getExpenseDate() != null) {
            expense.setExpenseDate(request.getExpenseDate());
        }
        if (request.getSprintId() != null) {
            checkSprint(expense.getProjectId(), request.getSprintId());
            expense.setSprintId(request.getSprintId());
        }
        expense = expenseRepository.save(expense);

        return toResponse(expense);
    }

    @Override
    @Transactional
    public void deleteExpense(UUID expenseId, UUID userId) {
        ProjectExpense expense = findExpense(expenseId);
        Project project = projectAccess.project(expense.getProjectId());
        projectAccess.require(project, userId, Role.PROJECT_MANAGER);
        expenseRepository.delete(expense);
    }

    @Override
    @Transactional
    public SalaryResponse setSalary(UUID targetUserId, SalaryRequest request, UUID adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ForbiddenException("You don't have permission to do this"));
        if (admin.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only an admin can set a salary");
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (admin.getWorkspaceId() == null || !admin.getWorkspaceId().equals(target.getWorkspaceId())) {
            throw new ResourceNotFoundException("User not found");
        }

        UserSalary salary = salaryRepository.findByUserId(targetUserId).orElse(null);
        if (salary == null) {
            salary = new UserSalary();
            salary.setUserId(targetUserId);
        }
        salary.setMonthlySalary(request.getMonthlySalary());
        salary = salaryRepository.save(salary);

        return SalaryResponse.builder()
                .userId(targetUserId)
                .monthlySalary(salary.getMonthlySalary())
                .build();
    }

    private Instant costEnd(Project project, Instant now) {
        if (project.getStatus() == ProjectStatus.COMPLETED) {
            Instant end = project.getUpdatedAt();
            if (project.getCompleteDate() != null) {
                end = startOfDay(project.getCompleteDate());
            }
            if (end.isAfter(now)) {
                return now;
            }
            return end;
        }
        if (project.getStatus() == ProjectStatus.CANCELLED) {
            return project.getUpdatedAt();
        }
        return now;
    }

    private Map<UUID, Instant> startDates(Project project, List<ProjectMember> members) {
        Map<UUID, Instant> starts = new LinkedHashMap<>();
        for (ProjectMember member : members) {
            starts.put(member.getUserId(), member.getCreatedAt());
        }
        if (project.getProjectManagerId() != null && !starts.containsKey(project.getProjectManagerId())) {
            starts.put(project.getProjectManagerId(), project.getCreatedAt());
        }
        if (project.getTeamLeadId() != null && !starts.containsKey(project.getTeamLeadId())) {
            starts.put(project.getTeamLeadId(), project.getCreatedAt());
        }
        return starts;
    }

    private Map<UUID, BigDecimal> salariesOf(Set<UUID> userIds) {
        Map<UUID, BigDecimal> salaries = new HashMap<>();
        if (userIds.isEmpty()) {
            return salaries;
        }
        for (UserSalary salary : salaryRepository.findByUserIdIn(userIds)) {
            salaries.put(salary.getUserId(), salary.getMonthlySalary());
        }
        return salaries;
    }

    private Map<UUID, String> userNames(Set<UUID> userIds) {
        Map<UUID, String> names = new HashMap<>();
        for (User user : userRepository.findAllById(userIds)) {
            names.put(user.getId(), user.getName());
        }
        return names;
    }

    private BigDecimal salaryFor(BigDecimal monthly, Instant from, Instant to) {
        long days = daysBetween(from, to);
        return monthly.multiply(BigDecimal.valueOf(days)).divide(DAYS_PER_MONTH, 2, RoundingMode.HALF_UP);
    }

    private long daysBetween(Instant from, Instant to) {
        long days = ChronoUnit.DAYS.between(from, to);
        if (days < 0) {
            return 0;
        }
        return days;
    }

    private Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private void checkSprint(UUID projectId, UUID sprintId) {
        if (sprintId == null) {
            return;
        }
        Sprint sprint = sprintRepository.findById(sprintId).orElse(null);
        if (sprint == null || !sprint.getProjectId().equals(projectId)) {
            throw new BadRequestException("Sprint does not belong to this project");
        }
    }

    private ProjectExpense findExpense(UUID expenseId) {
        return expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
    }

    private ExpenseResponse toResponse(ProjectExpense expense) {
        return ExpenseResponse.builder()
                .id(expense.getId())
                .projectId(expense.getProjectId())
                .category(expense.getCategory())
                .title(expense.getTitle())
                .amount(expense.getAmount())
                .expenseDate(expense.getExpenseDate())
                .sprintId(expense.getSprintId())
                .createdBy(expense.getCreatedBy())
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .build();
    }
}