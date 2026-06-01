package com.jnrptt.notificationsystemkafka.service;

import com.jnrptt.notificationsystemkafka.dto.BudgetRequestDTO;
import com.jnrptt.notificationsystemkafka.exception.DuplicateResourceException;
import com.jnrptt.notificationsystemkafka.model.User;
import com.jnrptt.notificationsystemkafka.repository.BudgetRepository;
import com.jnrptt.notificationsystemkafka.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BudgetService budgetService;

    @Test
    void createBudgetRejectsDuplicateCombination() {
        BudgetRequestDTO dto = new BudgetRequestDTO(1L, "Food", BigDecimal.valueOf(100), "2026-06");
        User user = new User(1L, "Juan", "juan@example.com", null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(budgetRepository.existsByUserIdAndCategoryAndMonth(1L, "food", "2026-06")).thenReturn(true);

        assertThatThrownBy(() -> budgetService.createBudget(dto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }
}
