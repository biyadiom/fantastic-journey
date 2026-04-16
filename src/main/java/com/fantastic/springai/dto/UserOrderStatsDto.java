package com.fantastic.springai.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserOrderStatsDto(
        Long userId,
        String username,
        Long orderCount,
        BigDecimal totalSpent,
        LocalDateTime lastOrderDate) {
}
