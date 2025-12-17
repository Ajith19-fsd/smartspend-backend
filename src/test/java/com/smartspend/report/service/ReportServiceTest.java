package com.smartspend.report.service;

import com.smartspend.budget.model.Budget;
import com.smartspend.budget.repository.BudgetRepository;
import com.smartspend.expense.model.Expense;
import com.smartspend.expense.repository.ExpenseRepository;
import com.smartspend.report.dto.CategorySummaryDto;
import com.smartspend.report.dto.MonthlySummaryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReportServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @InjectMocks
    private ReportService reportService;

    private List<Expense> sampleExpenses;
    private List<Budget> sampleBudgets;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        sampleExpenses = new ArrayList<>();
        sampleExpenses.add(createExpense(1L, "Salary", 5000.0, "Income", "INCOME", LocalDate.of(2025, 12, 1)));
        sampleExpenses.add(createExpense(1L, "Groceries", 200.0, "Food", "EXPENSE", LocalDate.of(2025, 12, 2)));
        sampleExpenses.add(createExpense(1L, "Electricity", 100.0, "Bills", "EXPENSE", LocalDate.of(2025, 12, 5)));

        sampleBudgets = new ArrayList<>();
        sampleBudgets.add(new Budget(1L, "Food", 500.0));
        sampleBudgets.add(new Budget(1L, "Bills", 300.0));
    }

    private Expense createExpense(Long id, String title, Double amount, String category, String type, LocalDate date) {
        Expense e = new Expense();
        e.setId(id);
        e.setUserId(1L);
        e.setTitle(title);
        e.setAmount(amount);
        e.setCategory(category);
        e.setType(type);
        e.setDate(date);
        return e;
    }

    @Test
    void testGetExpensesReport_NoFilter() {
        when(expenseRepository.findByUserId(1L)).thenReturn(sampleExpenses);

        List<Expense> result = reportService.getExpensesReport(1L, null, null, null);

        assertEquals(3, result.size());
        verify(expenseRepository, times(1)).findByUserId(1L);
    }

    @Test
    void testGetExpensesReport_ByCategory() {
        when(expenseRepository.findByUserIdAndCategory(1L, "Food")).thenReturn(Collections.singletonList(sampleExpenses.get(1)));

        List<Expense> result = reportService.getExpensesReport(1L, "Food", null, null);

        assertEquals(1, result.size());
        assertEquals("Groceries", result.get(0).getTitle());
    }

    @Test
    void testGetExpensesReport_ByDateRange() {
        when(expenseRepository.findByUserIdAndDateBetween(1L, LocalDate.of(2025,12,1), LocalDate.of(2025,12,5)))
                .thenReturn(sampleExpenses);

        List<Expense> result = reportService.getExpensesReport(1L, null, LocalDate.of(2025,12,1), LocalDate.of(2025,12,5));

        assertEquals(3, result.size());
    }

    @Test
    void testGetDashboardSummary() {
        when(expenseRepository.findByUserId(1L)).thenReturn(sampleExpenses);
        when(budgetRepository.findByUserId(1L)).thenReturn(sampleBudgets);

        Map<String, Double> summary = reportService.getDashboardSummary(1L);

        assertEquals(300.0, summary.get("totalExpenses"));
        assertEquals(5000.0, summary.get("totalIncome"));
        assertEquals(800.0, summary.get("totalBudget"));
        assertEquals(500.0, summary.get("remaining")); // 800 - 300
    }

    @Test
    void testGetMonthlySummary() {
        when(expenseRepository.findByUserIdAndDateBetween(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(sampleExpenses);

        MonthlySummaryDto summary = reportService.getMonthlySummary(1L, 2025, 12);

        assertEquals(3, summary.getCategories().size());
        assertEquals(300.0, summary.getTotalExpense());
        assertEquals(5000.0, summary.getTotalIncome());
    }

    @Test
    void testGetCategorySummary() {
        when(expenseRepository.findByUserIdAndDateBetween(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(sampleExpenses);

        List<CategorySummaryDto> categories = reportService.getCategorySummary(1L, 2025, 12);

        assertEquals(3, categories.size());
    }

    @Test
    void testGetIncomeExpenseSummary() {
        when(expenseRepository.findByUserIdAndDateBetween(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(sampleExpenses);

        Map<String, Double> result = reportService.getIncomeExpenseSummary(1L, 2025, 12);

        assertEquals(5000.0, result.get("income"));
        assertEquals(300.0, result.get("expense"));
        assertEquals(4700.0, result.get("balance"));
    }

    @Test
    void testGetMonthlyTrend() {
        for (int month = 1; month <= 12; month++) {
            when(expenseRepository.findByUserIdAndDateBetween(eq(1L),
                    any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(sampleExpenses);
        }

        Map<String, Map<String, Double>> trend = reportService.getMonthlyTrend(1L, 2025);

        assertEquals(12, trend.size());
        trend.values().forEach(monthData -> {
            assertEquals(5000.0, monthData.get("income"));
            assertEquals(300.0, monthData.get("expense"));
        });
    }
}
