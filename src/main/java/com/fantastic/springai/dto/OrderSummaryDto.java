package com.fantastic.springai.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fantastic.springai.model.OrderStatus;

public record OrderSummaryDto(
        Long id,
        LocalDateTime createdAt,
        OrderStatus status,
        BigDecimal totalAmount,
        String currency,
        String userEmail) {
}
