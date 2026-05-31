package com.jnrptt.notificationsystemkafka.service;

import com.jnrptt.notificationsystemkafka.dto.ExpenseRequestDTO;
import com.jnrptt.notificationsystemkafka.dto.ExpenseResponseDTO;
import com.jnrptt.notificationsystemkafka.kafka.event.BudgetExceededEvent;
import com.jnrptt.notificationsystemkafka.kafka.event.ExpenseCreatedEvent;
import com.jnrptt.notificationsystemkafka.kafka.producer.NotificationProducer;
import com.jnrptt.notificationsystemkafka.model.Expense;
import com.jnrptt.notificationsystemkafka.model.User;
import com.jnrptt.notificationsystemkafka.repository.BudgetRepository;
import com.jnrptt.notificationsystemkafka.repository.ExpenseRepository;
import com.jnrptt.notificationsystemkafka.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExpenseService {
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final NotificationProducer notificationProducer;
    private final BudgetRepository budgetRepository;

    private static final Logger log = LoggerFactory.getLogger(ExpenseService.class);

    public List<ExpenseResponseDTO> getAllExpenses() {
        return expenseRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public Optional<ExpenseResponseDTO> getExpenseById(Long id) {
        return expenseRepository.findById(id)
                .map(this::toResponseDTO);
    }

    @Transactional
    public Optional<ExpenseResponseDTO> createExpense(ExpenseRequestDTO dto) {
        if (dto.getUserId() == null) {
            return Optional.empty();
        }
        return userRepository.findById(dto.getUserId())
                .map(user -> {
                    Expense saved = expenseRepository.save(buildExpense(new Expense(), dto, user));

                    try {
                        notificationProducer.sendExpenseCreated(new ExpenseCreatedEvent(
                                saved.getId(),
                                user.getId(),
                                user.getEmail(),
                                saved.getDescription(),
                                saved.getCategory(),
                                saved.getAmount(),
                                saved.getDate()
                        ));
                    } catch (Exception e) {
                        log.error("Error enviando ExpenseCreatedEvent a Kafka. expenseId={}, userId={}",
                                saved.getId(), user.getId(), e);
                        throw e;
                    }

                    String month = saved.getDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
                    LocalDate startDate = saved.getDate().withDayOfMonth(1);
                    LocalDate endDate = startDate.plusMonths(1);

                    budgetRepository.findByUserIdAndCategoryAndMonth(saved.getUser().getId(), saved.getCategory(), month)
                            .ifPresent(budget -> {
                                if (budget.getLimitAmount() == null) {
                                    log.warn("Budget sin limitAmount. budgetId={}, userId={}, category={}, month={}",
                                            budget.getId(), saved.getUser().getId(), saved.getCategory(), month);
                                    return;
                                }

                                BigDecimal total = expenseRepository.sumByUserAndCategoryBetweenDates(
                                        saved.getUser().getId(), saved.getCategory(), startDate, endDate
                                );

                                if (total.compareTo(budget.getLimitAmount()) > 0) {
                                    notificationProducer.sendBudgetExceeded(new BudgetExceededEvent(
                                            saved.getUser().getId(),
                                            saved.getUser().getEmail(),
                                            saved.getCategory(),
                                            budget.getLimitAmount(),
                                            total,
                                            month
                                    ));
                                }
                            });

                    return toResponseDTO(saved);
                });
    }

    @Transactional
    public Optional<ExpenseResponseDTO> updateExpense(Long id, ExpenseRequestDTO dto) {
        if (dto.getUserId() == null) {
            return Optional.empty();
        }
        Optional<Expense> existingOpt = expenseRepository.findById(id);
        Optional<User> userOpt = userRepository.findById(dto.getUserId());

        if (existingOpt.isEmpty() || userOpt.isEmpty()) {
            return Optional.empty();
        }

        Expense existing = existingOpt.get();
        return Optional.of(toResponseDTO(expenseRepository.save(buildExpense(existing, dto, userOpt.get()))));
    }

    @Transactional
    public boolean deleteExpenseById(Long id) {
        if (!expenseRepository.existsById(id)) {
            return false;
        }
        expenseRepository.deleteById(id);
        return true;
    }

    private Expense buildExpense(Expense expense, ExpenseRequestDTO dto, User user) {
        expense.setUser(user);
        expense.setAmount(dto.getAmount());
        expense.setDescription(dto.getDescription());
        expense.setCategory(dto.getCategory());
        expense.setDate(dto.getDate() != null ? dto.getDate() : LocalDate.now());
        return expense;
    }

    private ExpenseResponseDTO toResponseDTO(Expense expense) {
        return new ExpenseResponseDTO(
                expense.getId(),
                expense.getUser().getId(),
                expense.getDescription(),
                expense.getCategory(),
                expense.getAmount(),
                expense.getDate()
        );
    }
}
