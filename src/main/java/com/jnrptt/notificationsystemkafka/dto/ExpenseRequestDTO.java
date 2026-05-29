package com.jnrptt.notificationsystemkafka.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExpenseRequestDTO {
    private Long userId;
    private String description;
    private String category;
    private BigDecimal amount;
    private LocalDate date;
}
