package com.jnrptt.notificationsystemkafka.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BudgetRequestDTO {
    @NotNull(message = "User ID cannot be null")
    private Long userId;

    @NotBlank(message = "Category cannot be blank")
    @Size(max = 80, message = "Category cannot exceed 80 characters")
    private String category;

    @NotNull(message = "Limit amount cannot be null")
    @Positive(message = "Limit amount must be positive")
    private BigDecimal limitAmount;

    @NotBlank(message = "Month cannot be blank")
    @Pattern(
            regexp = "^\\d{4}-(0[1-9]|1[0-2])$",
            message = "Month must have format yyyy-MM"
    )
    private String month;
}
