package com.jnrptt.notificationsystemkafka.repository;

import com.jnrptt.notificationsystemkafka.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
}
