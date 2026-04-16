package com.fantastic.springai.dto;

import java.math.BigDecimal;

public record OrderItemDto(
        String productName,
        String sku,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal discount,
        BigDecimal lineTotal) {
}
