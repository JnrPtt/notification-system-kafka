package com.jnrptt.notificationsystemkafka.service;

import com.jnrptt.notificationsystemkafka.dto.ExpenseRequestDTO;
import com.jnrptt.notificationsystemkafka.dto.ExpenseResponseDTO;
import com.jnrptt.notificationsystemkafka.exception.ResourceNotFoundException;
import com.jnrptt.notificationsystemkafka.kafka.event.BudgetExceededEvent;
import com.jnrptt.notificationsystemkafka.kafka.event.ExpenseCreatedEvent;
import com.jnrptt.notificationsystemkafka.kafka.producer.NotificationProducer;
import com.jnrptt.notificationsystemkafka.model.Budget;
import com.jnrptt.notificationsystemkafka.model.Expense;
import com.jnrptt.notificationsystemkafka.model.User;
import com.jnrptt.notificationsystemkafka.repository.BudgetRepository;
import com.jnrptt.notificationsystemkafka.repository.ExpenseRepository;
import com.jnrptt.notificationsystemkafka.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    private static final LocalDate JUNE_15 = LocalDate.of(2026, 6, 15);

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationProducer notificationProducer;

    @Mock
    private BudgetRepository budgetRepository;

    @InjectMocks
    private ExpenseService expenseService;

    private final User user = new User(1L, "Juan", "juan@example.com", null);

    // ---------- create ----------

    @Test
    void createExpenseRejectsMissingUser() {
        ExpenseRequestDTO dto = new ExpenseRequestDTO(99L, "Lunch", "Food", BigDecimal.valueOf(12.50), null);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.createExpense(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void createExpenseNormalizesFieldsBeforeSaving() {
        ExpenseRequestDTO dto = new ExpenseRequestDTO(1L, "  Lunch  ", "  Food ", BigDecimal.TEN, JUNE_15);
        givenUserAndSaveWithId(10L);

        expenseService.createExpense(dto);

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(captor.capture());
        Expense saved = captor.getValue();
        assertThat(saved.getCategory()).isEqualTo("food");
        assertThat(saved.getDescription()).isEqualTo("Lunch");
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getAmount()).isEqualByComparingTo("10");
        assertThat(saved.getDate()).isEqualTo(JUNE_15);
    }

    @Test
    void createExpenseDefaultsDateToToday() {
        ExpenseRequestDTO dto = new ExpenseRequestDTO(1L, null, "food", BigDecimal.TEN, null);
        givenUserAndSaveWithId(10L);

        ExpenseResponseDTO response = expenseService.createExpense(dto).orElseThrow();

        assertThat(response.getDate()).isEqualTo(LocalDate.now());
        assertThat(response.getDescription()).isNull();
    }

    @Test
    void createExpensePublishesExpenseCreatedEvent() {
        ExpenseRequestDTO dto = new ExpenseRequestDTO(1L, "Lunch", "Food", BigDecimal.valueOf(25.5), JUNE_15);
        givenUserAndSaveWithId(10L);

        expenseService.createExpense(dto);

        ArgumentCaptor<ExpenseCreatedEvent> captor = ArgumentCaptor.forClass(ExpenseCreatedEvent.class);
        verify(notificationProducer).sendExpenseCreated(captor.capture());
        ExpenseCreatedEvent event = captor.getValue();
        assertThat(event.getExpenseId()).isEqualTo(10L);
        assertThat(event.getUserId()).isEqualTo(1L);
        assertThat(event.getUserEmail()).isEqualTo("juan@example.com");
        assertThat(event.getDescription()).isEqualTo("Lunch");
        assertThat(event.getCategory()).isEqualTo("food");
        assertThat(event.getAmount()).isEqualByComparingTo("25.5");
        assertThat(event.getDate()).isEqualTo(JUNE_15);
    }

    @Test
    void createExpenseRethrowsWhenKafkaFailsAndSkipsBudgetCheck() {
        ExpenseRequestDTO dto = new ExpenseRequestDTO(1L, "Lunch", "food", BigDecimal.TEN, JUNE_15);
        givenUserAndSaveWithId(10L);
        doThrow(new IllegalStateException("kafka down")).when(notificationProducer).sendExpenseCreated(any());

        assertThatThrownBy(() -> expenseService.createExpense(dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("kafka down");

        verifyNoInteractions(budgetRepository);
    }

    // ---------- budget check ----------

    @Test
    void createExpenseWithoutBudgetDoesNotPublishBudgetExceeded() {
        ExpenseRequestDTO dto = new ExpenseRequestDTO(1L, "Lunch", "food", BigDecimal.TEN, JUNE_15);
        givenUserAndSaveWithId(10L);
        when(budgetRepository.findByUserIdAndCategoryAndMonth(1L, "food", "2026-06")).thenReturn(Optional.empty());

        expenseService.createExpense(dto);

        verify(expenseRepository, never()).sumByUserAndCategoryBetweenDates(anyLong(), anyString(), any(), any());
        verify(notificationProducer, never()).sendBudgetExceeded(any());
    }

    @Test
    void createExpensePublishesBudgetExceededWhenTotalIsAboveLimit() {
        ExpenseRequestDTO dto = new ExpenseRequestDTO(1L, "Lunch", "Food", BigDecimal.TEN, JUNE_15);
        givenUserAndSaveWithId(10L);
        givenBudget("100.00");
        givenMonthlyTotal("100.01");

        expenseService.createExpense(dto);

        ArgumentCaptor<BudgetExceededEvent> captor = ArgumentCaptor.forClass(BudgetExceededEvent.class);
        verify(notificationProducer).sendBudgetExceeded(captor.capture());
        BudgetExceededEvent event = captor.getValue();
        assertThat(event.getUserId()).isEqualTo(1L);
        assertThat(event.getUserEmail()).isEqualTo("juan@example.com");
        assertThat(event.getCategory()).isEqualTo("food");
        assertThat(event.getLimitAmount()).isEqualByComparingTo("100.00");
        assertThat(event.getTotal()).isEqualByComparingTo("100.01");
        assertThat(event.getMonth()).isEqualTo("2026-06");
    }

    @Test
    void createExpenseDoesNotPublishBudgetExceededWhenTotalEqualsLimit() {
        ExpenseRequestDTO dto = new ExpenseRequestDTO(1L, "Lunch", "food", BigDecimal.TEN, JUNE_15);
        givenUserAndSaveWithId(10L);
        givenBudget("100.00");
        givenMonthlyTotal("100.00");

        expenseService.createExpense(dto);

        verify(notificationProducer, never()).sendBudgetExceeded(any());
    }

    @Test
    void createExpenseDoesNotPublishBudgetExceededWhenTotalIsBelowLimit() {
        ExpenseRequestDTO dto = new ExpenseRequestDTO(1L, "Lunch", "food", BigDecimal.TEN, JUNE_15);
        givenUserAndSaveWithId(10L);
        givenBudget("100.00");
        givenMonthlyTotal("99.99");

        expenseService.createExpense(dto);

        verify(notificationProducer, never()).sendBudgetExceeded(any());
    }

    @Test
    void createExpenseIgnoresBudgetWithoutLimitAmount() {
        ExpenseRequestDTO dto = new ExpenseRequestDTO(1L, "Lunch", "food", BigDecimal.TEN, JUNE_15);
        givenUserAndSaveWithId(10L);
        Budget budget = new Budget(5L, user, "food", null, "2026-06");
        when(budgetRepository.findByUserIdAndCategoryAndMonth(1L, "food", "2026-06")).thenReturn(Optional.of(budget));

        expenseService.createExpense(dto);

        verify(expenseRepository, never()).sumByUserAndCategoryBetweenDates(anyLong(), anyString(), any(), any());
        verify(notificationProducer, never()).sendBudgetExceeded(any());
    }

    @Test
    void budgetCheckSumsExpensesOfTheExpenseMonth() {
        ExpenseRequestDTO dto = new ExpenseRequestDTO(1L, "Lunch", "food", BigDecimal.TEN, JUNE_15);
        givenUserAndSaveWithId(10L);
        givenBudget("100.00");
        when(expenseRepository.sumByUserAndCategoryBetweenDates(
                1L, "food", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 1)))
                .thenReturn(BigDecimal.ZERO);

        expenseService.createExpense(dto);

        verify(expenseRepository).sumByUserAndCategoryBetweenDates(
                1L, "food", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 1));
    }

    @Test
    void budgetCheckRollsDecemberOverToNextYear() {
        LocalDate december = LocalDate.of(2025, 12, 31);
        ExpenseRequestDTO dto = new ExpenseRequestDTO(1L, "Gift", "food", BigDecimal.TEN, december);
        givenUserAndSaveWithId(10L);
        Budget budget = new Budget(5L, user, "food", new BigDecimal("100.00"), "2025-12");
        when(budgetRepository.findByUserIdAndCategoryAndMonth(1L, "food", "2025-12")).thenReturn(Optional.of(budget));
        when(expenseRepository.sumByUserAndCategoryBetweenDates(
                1L, "food", LocalDate.of(2025, 12, 1), LocalDate.of(2026, 1, 1)))
                .thenReturn(BigDecimal.ZERO);

        expenseService.createExpense(dto);

        verify(expenseRepository).sumByUserAndCategoryBetweenDates(
                1L, "food", LocalDate.of(2025, 12, 1), LocalDate.of(2026, 1, 1));
    }

    // ---------- update ----------

    @Test
    void updateExpenseReturnsEmptyWhenExpenseDoesNotExist() {
        when(expenseRepository.findById(7L)).thenReturn(Optional.empty());

        Optional<ExpenseResponseDTO> result = expenseService.updateExpense(7L,
                new ExpenseRequestDTO(1L, "x", "food", BigDecimal.TEN, JUNE_15));

        assertThat(result).isEmpty();
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void updateExpenseRejectsMissingUser() {
        Expense existing = new Expense(7L, BigDecimal.ONE, "old", JUNE_15, "food", user);
        when(expenseRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.updateExpense(7L,
                new ExpenseRequestDTO(99L, "x", "food", BigDecimal.TEN, JUNE_15)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(expenseRepository, never()).save(any());
    }

    @Test
    void updateExpenseChecksBudgetButDoesNotPublishCreatedEvent() {
        Expense existing = new Expense(7L, BigDecimal.ONE, "old", JUNE_15, "food", user);
        when(expenseRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));
        givenBudget("100.00");
        givenMonthlyTotal("150.00");

        ExpenseResponseDTO response = expenseService.updateExpense(7L,
                new ExpenseRequestDTO(1L, "New", "Food", BigDecimal.valueOf(150), JUNE_15)).orElseThrow();

        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getDescription()).isEqualTo("New");
        verify(notificationProducer, never()).sendExpenseCreated(any());
        verify(notificationProducer).sendBudgetExceeded(any(BudgetExceededEvent.class));
    }

    // ---------- delete ----------

    @Test
    void deleteExpenseRemovesExistingExpense() {
        when(expenseRepository.existsById(7L)).thenReturn(true);

        assertThat(expenseService.deleteExpenseById(7L)).isTrue();

        verify(expenseRepository).deleteById(7L);
    }

    @Test
    void deleteExpenseReturnsFalseWhenMissing() {
        when(expenseRepository.existsById(7L)).thenReturn(false);

        assertThat(expenseService.deleteExpenseById(7L)).isFalse();

        verify(expenseRepository, never()).deleteById(anyLong());
    }

    // ---------- helpers ----------

    private void givenUserAndSaveWithId(Long expenseId) {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
            Expense expense = invocation.getArgument(0);
            expense.setId(expenseId);
            return expense;
        });
    }

    private void givenBudget(String limit) {
        Budget budget = new Budget(5L, user, "food", new BigDecimal(limit), "2026-06");
        when(budgetRepository.findByUserIdAndCategoryAndMonth(1L, "food", "2026-06")).thenReturn(Optional.of(budget));
    }

    private void givenMonthlyTotal(String total) {
        when(expenseRepository.sumByUserAndCategoryBetweenDates(
                1L, "food", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 1)))
                .thenReturn(new BigDecimal(total));
    }
}
