package com.jnrptt.notificationsystemkafka.service;

import com.jnrptt.notificationsystemkafka.dto.BudgetRequestDTO;
import com.jnrptt.notificationsystemkafka.dto.BudgetResponseDTO;
import com.jnrptt.notificationsystemkafka.model.Budget;
import com.jnrptt.notificationsystemkafka.model.User;
import com.jnrptt.notificationsystemkafka.repository.BudgetRepository;
import com.jnrptt.notificationsystemkafka.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BudgetService {
    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;

    public List<BudgetResponseDTO> getAllBudgets() {
        return budgetRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public Optional<BudgetResponseDTO> getBudgetById(Long id) {
        return budgetRepository.findById(id)
                .map(this::toResponseDTO);
    }

    @Transactional
    public Optional<BudgetResponseDTO> createBudget(BudgetRequestDTO dto) {
        if (dto.getUserId() == null) {
            return Optional.empty();
        }
        return userRepository.findById(dto.getUserId())
                .map(user -> toResponseDTO(budgetRepository.save(buildBudget(new Budget(), dto, user))));
    }

    @Transactional
    public Optional<BudgetResponseDTO> updateBudget(Long id, BudgetRequestDTO dto) {
        if (dto.getUserId() == null) {
            return Optional.empty();
        }
        Optional<Budget> existingOpt = budgetRepository.findById(id);
        Optional<User> userOpt = userRepository.findById(dto.getUserId());

        if (existingOpt.isEmpty() || userOpt.isEmpty()) {
            return Optional.empty();
        }

        Budget existing = existingOpt.get();
        return Optional.of(toResponseDTO(budgetRepository.save(buildBudget(existing, dto, userOpt.get()))));
    }

    @Transactional
    public boolean deleteBudgetById(Long id) {
        if (!budgetRepository.existsById(id)) {
            return false;
        }
        budgetRepository.deleteById(id);
        return true;
    }

    private Budget buildBudget(Budget budget, BudgetRequestDTO dto, User user) {
        budget.setUser(user);
        budget.setCategory(dto.getCategory());
        budget.setLimitAmount(dto.getLimitAmount());
        budget.setMonth(dto.getMonth());
        return budget;
    }

    private BudgetResponseDTO toResponseDTO(Budget budget) {
        return new BudgetResponseDTO(
                budget.getId(),
                budget.getUser().getId(),
                budget.getCategory(),
                budget.getLimitAmount(),
                budget.getMonth()
        );
    }
}
