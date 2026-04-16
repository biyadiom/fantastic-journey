package com.fantastic.springai.dto;

import java.math.BigDecimal;

public record ProductSalesDto(
        Long productId,
        String productName,
        Long totalQuantitySold,
        BigDecimal totalRevenue) {
}
