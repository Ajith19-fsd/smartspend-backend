package com.smartspend.budget.service;

import com.smartspend.budget.model.Budget;
import com.smartspend.budget.repository.BudgetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BudgetServiceTest {

    @InjectMocks
    private BudgetService budgetService;

    @Mock
    private BudgetRepository budgetRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ====== saveBudget ======
    @Test
    void testSaveBudget_success() {
        Budget budget = new Budget(1L, "Food", 5000.0);

        when(budgetRepository.save(budget)).thenReturn(budget);

        Budget saved = budgetService.saveBudget(budget);

        assertNotNull(saved);
        assertEquals("Food", saved.getCategory());
        verify(budgetRepository, times(1)).save(budget);
    }

    @Test
    void testSaveBudget_noUserId_throwsException() {
        Budget budget = new Budget(null, "Travel", 2000.0);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> budgetService.saveBudget(budget));
        assertEquals("Cannot create budget without userId", ex.getMessage());
        verify(budgetRepository, never()).save(any());
    }

    // ====== getBudgets ======
    @Test
    void testGetBudgets_returnsList() {
        List<Budget> budgets = Arrays.asList(
                new Budget(1L, "Food", 5000.0),
                new Budget(1L, "Travel", 2000.0)
        );
        when(budgetRepository.findByUserId(1L)).thenReturn(budgets);

        List<Budget> result = budgetService.getBudgets(1L);

        assertEquals(2, result.size());
        verify(budgetRepository, times(1)).findByUserId(1L);
    }

    // ====== getBudgetById ======
    @Test
    void testGetBudgetById_found() {
        Budget budget = new Budget(1L, "Food", 5000.0);
        budget.setId(10L);

        when(budgetRepository.findById(10L)).thenReturn(Optional.of(budget));

        Budget result = budgetService.getBudgetById(10L, 1L);
        assertNotNull(result);
        assertEquals(10L, result.getId());
    }

    @Test
    void testGetBudgetById_wrongUser_returnsNull() {
        Budget budget = new Budget(2L, "Food", 5000.0);
        budget.setId(10L);

        when(budgetRepository.findById(10L)).thenReturn(Optional.of(budget));

        Budget result = budgetService.getBudgetById(10L, 1L);
        assertNull(result);
    }

    // ====== getBudgetByCategory ======
    @Test
    void testGetBudgetByCategory_found() {
        Budget budget = new Budget(1L, "Food", 5000.0);
        when(budgetRepository.findByUserIdAndCategoryIgnoreCase(1L, "Food"))
                .thenReturn(Optional.of(budget));

        Budget result = budgetService.getBudgetByCategory(1L, "Food");
        assertNotNull(result);
        assertEquals("Food", result.getCategory());
    }

    @Test
    void testGetBudgetByCategory_notFound_returnsNull() {
        when(budgetRepository.findByUserIdAndCategoryIgnoreCase(1L, "Travel"))
                .thenReturn(Optional.empty());

        Budget result = budgetService.getBudgetByCategory(1L, "Travel");
        assertNull(result);
    }

    // ====== updateBudget ======
    @Test
    void testUpdateBudget_success() {
        Budget existing = new Budget(1L, "Food", 5000.0);
        existing.setId(10L);

        Budget updated = new Budget(1L, "Food", 6000.0);
        updated.setId(10L);

        when(budgetRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(budgetRepository.save(any(Budget.class))).thenReturn(updated);

        Budget result = budgetService.updateBudget(10L, updated);

        assertNotNull(result);
        assertEquals(6000.0, result.getAmount());
    }

    @Test
    void testUpdateBudget_wrongUser_returnsNull() {
        Budget existing = new Budget(2L, "Food", 5000.0);
        existing.setId(10L);

        Budget updated = new Budget(1L, "Food", 6000.0);
        updated.setId(10L);

        when(budgetRepository.findById(10L)).thenReturn(Optional.of(existing));

        Budget result = budgetService.updateBudget(10L, updated);
        assertNull(result);
    }

    // ====== deleteBudget ======
    @Test
    void testDeleteBudget_success() {
        Budget budget = new Budget(1L, "Food", 5000.0);
        budget.setId(10L);

        when(budgetRepository.findById(10L)).thenReturn(Optional.of(budget));

        boolean result = budgetService.deleteBudget(10L, 1L);

        assertTrue(result);
        verify(budgetRepository, times(1)).delete(budget);
    }

    @Test
    void testDeleteBudget_wrongUser_returnsFalse() {
        Budget budget = new Budget(2L, "Food", 5000.0);
        budget.setId(10L);

        when(budgetRepository.findById(10L)).thenReturn(Optional.of(budget));

        boolean result = budgetService.deleteBudget(10L, 1L);

        assertFalse(result);
        verify(budgetRepository, never()).delete(any());
    }
}
