package com.jnrptt.notificationsystemkafka.repository;

import com.jnrptt.notificationsystemkafka.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
}
