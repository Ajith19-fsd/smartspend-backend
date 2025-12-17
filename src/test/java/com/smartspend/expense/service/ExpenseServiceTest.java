package com.smartspend.expense.service;

import com.smartspend.budget.model.Budget;
import com.smartspend.budget.service.BudgetService;
import com.smartspend.email.EmailService;
import com.smartspend.expense.model.Expense;
import com.smartspend.expense.repository.ExpenseRepository;
import com.smartspend.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private BudgetService budgetService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ExpenseService expenseService;

    private Expense expense;

    @BeforeEach
    void setUp() {
        expense = new Expense();
        expense.setId(1L);
        expense.setUserId(10L);
        expense.setTitle("Lunch");
        expense.setAmount(200.0);
        expense.setCategory("Food");
        expense.setType("EXPENSE");
        expense.setDate(LocalDate.now());
        expense.setDescription("Office lunch");
    }

    // ✅ Test: Save Expense
    @Test
    void saveExpense_shouldSaveExpenseSuccessfully() {
        when(expenseRepository.save(any(Expense.class))).thenReturn(expense);
        when(budgetService.getBudgetByCategory(10L, "Food")).thenReturn(null);

        Expense saved = expenseService.saveExpense(expense);

        assertNotNull(saved);
        assertEquals("Lunch", saved.getTitle());
        verify(expenseRepository, times(1)).save(expense);
    }

    // ✅ Test: Save Expense without userId (Exception case)
    @Test
    void saveExpense_withoutUserId_shouldThrowException() {
        expense.setUserId(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> expenseService.saveExpense(expense));

        assertEquals("Expense must have a userId", ex.getMessage());
        verify(expenseRepository, never()).save(any());
    }

    // ✅ Test: Get All Expenses
    @Test
    void getAllExpenses_shouldReturnExpenseList() {
        when(expenseRepository.findByUserId(10L)).thenReturn(List.of(expense));

        List<Expense> expenses = expenseService.getAllExpenses(10L);

        assertEquals(1, expenses.size());
        verify(expenseRepository, times(1)).findByUserId(10L);
    }

    // ✅ Test: Get Expense By ID (Authorized)
    @Test
    void getExpenseById_validUser_shouldReturnExpense() {
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));

        Expense result = expenseService.getExpenseById(1L, 10L);

        assertNotNull(result);
        assertEquals(10L, result.getUserId());
    }

    // ✅ Test: Get Expense By ID (Unauthorized)
    @Test
    void getExpenseById_invalidUser_shouldThrowException() {
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> expenseService.getExpenseById(1L, 99L));

        assertEquals("Unauthorized access", ex.getMessage());
    }

    // ✅ Test: Update Expense
    @Test
    void updateExpense_shouldUpdateSuccessfully() {
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(any(Expense.class))).thenReturn(expense);
        when(budgetService.getBudgetByCategory(10L, "Food")).thenReturn(null);

        Expense updated = expenseService.updateExpense(1L, expense);

        assertNotNull(updated);
        verify(expenseRepository, times(1)).save(expense);
    }

    // ✅ Test: Delete Expense
    @Test
    void deleteExpense_shouldDeleteSuccessfully() {
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));

        expenseService.deleteExpense(1L, 10L);

        verify(expenseRepository, times(1)).delete(expense);
    }

    // ✅ Test: Filter Expenses
    @Test
    void filterExpenses_shouldFilterByCategory() {
        when(expenseRepository.findByUserId(10L)).thenReturn(List.of(expense));

        List<Expense> filtered = expenseService.filterExpenses(
                10L, "Food", null, null, null, null, null
        );

        assertEquals(1, filtered.size());
        assertEquals("Food", filtered.get(0).getCategory());
    }
}
