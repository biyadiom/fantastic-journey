package com.fantastic.springai.dto;

import java.math.BigDecimal;

public record RevenueStatsDto(
        BigDecimal totalRevenue,
        long orderCount,
        BigDecimal avgOrderValue,
        String period) {
}
