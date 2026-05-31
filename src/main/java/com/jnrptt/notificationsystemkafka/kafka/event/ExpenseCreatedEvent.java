package com.jnrptt.notificationsystemkafka.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExpenseCreatedEvent {
    private Long expenseId;
    private Long userId;
    private String userEmail;
    private String description;
    private String category;
    private BigDecimal amount;
    private LocalDate date;
}
