package com.jnrptt.notificationsystemkafka.service;

import com.jnrptt.notificationsystemkafka.dto.ExpenseRequestDTO;
import com.jnrptt.notificationsystemkafka.dto.ExpenseResponseDTO;
import com.jnrptt.notificationsystemkafka.model.Expense;
import com.jnrptt.notificationsystemkafka.model.User;
import com.jnrptt.notificationsystemkafka.repository.ExpenseRepository;
import com.jnrptt.notificationsystemkafka.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExpenseService {
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

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
                .map(user -> toResponseDTO(expenseRepository.save(buildExpense(new Expense(), dto, user))));
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
