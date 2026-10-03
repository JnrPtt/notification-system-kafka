package com.jnrptt.notificationsystemkafka.service;

import com.jnrptt.notificationsystemkafka.dto.BudgetRequestDTO;
import com.jnrptt.notificationsystemkafka.dto.BudgetResponseDTO;
import com.jnrptt.notificationsystemkafka.exception.DuplicateResourceException;
import com.jnrptt.notificationsystemkafka.exception.ResourceNotFoundException;
import com.jnrptt.notificationsystemkafka.model.Budget;
import com.jnrptt.notificationsystemkafka.model.User;
import com.jnrptt.notificationsystemkafka.repository.BudgetRepository;
import com.jnrptt.notificationsystemkafka.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BudgetService budgetService;

    private final User user = new User(1L, "Juan", "juan@example.com", null);

    // ---------- create ----------

    @Test
    void createBudgetRejectsMissingUser() {
        BudgetRequestDTO dto = new BudgetRequestDTO(99L, "Food", BigDecimal.valueOf(100), "2026-06");
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.createBudget(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void createBudgetRejectsDuplicateCombination() {
        BudgetRequestDTO dto = new BudgetRequestDTO(1L, "Food", BigDecimal.valueOf(100), "2026-06");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(budgetRepository.existsByUserIdAndCategoryAndMonth(1L, "food", "2026-06")).thenReturn(true);

        assertThatThrownBy(() -> budgetService.createBudget(dto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(budgetRepository, never()).save(any());
    }

    @Test
    void createBudgetNormalizesCategoryAndReturnsResponse() {
        BudgetRequestDTO dto = new BudgetRequestDTO(1L, "  Food ", new BigDecimal("300.00"), "2026-06");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(budgetRepository.existsByUserIdAndCategoryAndMonth(1L, "food", "2026-06")).thenReturn(false);
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
            Budget budget = invocation.getArgument(0);
            budget.setId(5L);
            return budget;
        });

        BudgetResponseDTO response = budgetService.createBudget(dto).orElseThrow();

        ArgumentCaptor<Budget> captor = ArgumentCaptor.forClass(Budget.class);
        verify(budgetRepository).save(captor.capture());
        assertThat(captor.getValue().getCategory()).isEqualTo("food");
        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getCategory()).isEqualTo("food");
        assertThat(response.getLimitAmount()).isEqualByComparingTo("300.00");
        assertThat(response.getMonth()).isEqualTo("2026-06");
    }

    // ---------- update ----------

    @Test
    void updateBudgetReturnsEmptyWhenBudgetDoesNotExist() {
        when(budgetRepository.findById(5L)).thenReturn(Optional.empty());

        Optional<BudgetResponseDTO> result = budgetService.updateBudget(5L,
                new BudgetRequestDTO(1L, "food", BigDecimal.TEN, "2026-06"));

        assertThat(result).isEmpty();
        verify(budgetRepository, never()).save(any());
    }

    @Test
    void updateBudgetRejectsCombinationUsedByAnotherBudget() {
        Budget existing = new Budget(5L, user, "food", BigDecimal.TEN, "2026-05");
        Budget other = new Budget(6L, user, "food", BigDecimal.TEN, "2026-06");
        when(budgetRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(budgetRepository.findByUserIdAndCategoryAndMonth(1L, "food", "2026-06")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> budgetService.updateBudget(5L,
                new BudgetRequestDTO(1L, "Food", BigDecimal.TEN, "2026-06")))
                .isInstanceOf(DuplicateResourceException.class);

        verify(budgetRepository, never()).save(any());
    }

    @Test
    void updateBudgetAllowsKeepingItsOwnCombination() {
        Budget existing = new Budget(5L, user, "food", BigDecimal.TEN, "2026-06");
        when(budgetRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(budgetRepository.findByUserIdAndCategoryAndMonth(1L, "food", "2026-06")).thenReturn(Optional.of(existing));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BudgetResponseDTO response = budgetService.updateBudget(5L,
                new BudgetRequestDTO(1L, "FOOD", new BigDecimal("250.00"), "2026-06")).orElseThrow();

        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getLimitAmount()).isEqualByComparingTo("250.00");
    }

    // ---------- delete ----------

    @Test
    void deleteBudgetRemovesExistingBudget() {
        when(budgetRepository.existsById(5L)).thenReturn(true);

        assertThat(budgetService.deleteBudgetById(5L)).isTrue();

        verify(budgetRepository).deleteById(5L);
    }

    @Test
    void deleteBudgetReturnsFalseWhenMissing() {
        when(budgetRepository.existsById(5L)).thenReturn(false);

        assertThat(budgetService.deleteBudgetById(5L)).isFalse();

        verify(budgetRepository, never()).deleteById(anyLong());
    }
}
