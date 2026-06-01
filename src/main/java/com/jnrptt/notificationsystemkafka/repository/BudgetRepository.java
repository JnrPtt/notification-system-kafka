package com.jnrptt.notificationsystemkafka.repository;

import com.jnrptt.notificationsystemkafka.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    Optional<Budget> findByUserIdAndCategoryAndMonth(Long userId, String category, String month);

    boolean existsByUserIdAndCategoryAndMonth(Long userId, String category, String month);
}
