package com.fantastic.springai.dto;

import java.math.BigDecimal;

public record CategoryStatsDto(
        Integer categoryId,
        String name,
        long productCount,
        BigDecimal totalSales) {
}
