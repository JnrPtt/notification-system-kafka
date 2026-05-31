package com.jnrptt.notificationsystemkafka.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BudgetExceededEvent {
    private Long userId;
    private String userEmail;
    private String category;
    private BigDecimal limitAmount;
    private BigDecimal total;
    private String month;
}
