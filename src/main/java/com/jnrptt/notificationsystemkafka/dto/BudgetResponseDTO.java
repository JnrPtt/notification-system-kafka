package com.jnrptt.notificationsystemkafka.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BudgetResponseDTO {
    private Long id;
    private Long userId;
    private String category;
    private BigDecimal limitAmount;
    private String month;
}
