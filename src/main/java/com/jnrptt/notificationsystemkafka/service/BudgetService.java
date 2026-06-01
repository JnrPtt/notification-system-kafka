package com.jnrptt.notificationsystemkafka.service;

import com.jnrptt.notificationsystemkafka.dto.BudgetRequestDTO;
import com.jnrptt.notificationsystemkafka.dto.BudgetResponseDTO;
import com.jnrptt.notificationsystemkafka.exception.DuplicateResourceException;
import com.jnrptt.notificationsystemkafka.exception.ResourceNotFoundException;
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
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String category = normalizeCategory(dto.getCategory());
        if (budgetRepository.existsByUserIdAndCategoryAndMonth(dto.getUserId(), category, dto.getMonth())) {
            throw new DuplicateResourceException("A budget already exists for this user, category and month");
        }

        return Optional.of(toResponseDTO(budgetRepository.save(buildBudget(new Budget(), dto, user))));
    }

    @Transactional
    public Optional<BudgetResponseDTO> updateBudget(Long id, BudgetRequestDTO dto) {
        Optional<Budget> existingOpt = budgetRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Budget existing = existingOpt.get();
        String category = normalizeCategory(dto.getCategory());
        budgetRepository.findByUserIdAndCategoryAndMonth(dto.getUserId(), category, dto.getMonth())
                .filter(budget -> !budget.getId().equals(id))
                .ifPresent(budget -> {
                    throw new DuplicateResourceException("A budget already exists for this user, category and month");
                });

        return Optional.of(toResponseDTO(budgetRepository.save(buildBudget(existing, dto, user))));
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
        budget.setCategory(normalizeCategory(dto.getCategory()));
        budget.setLimitAmount(dto.getLimitAmount());
        budget.setMonth(dto.getMonth());
        return budget;
    }

    private String normalizeCategory(String category) {
        return category.trim().toLowerCase();
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
