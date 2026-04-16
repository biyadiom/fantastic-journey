package com.fantastic.springai.dto;

import java.math.BigDecimal;

public record ProductSummaryDto(
        Long id,
        String name,
        String sku,
        BigDecimal basePrice,
        Integer stock,
        String categoryName,
        String sellerName,
        boolean active) {
}
