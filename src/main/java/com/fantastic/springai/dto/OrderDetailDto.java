package com.fantastic.springai.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fantastic.springai.model.OrderStatus;

public record OrderDetailDto(
        Long id,
        OrderStatus status,
        BigDecimal totalAmount,
        String currency,
        String notes,
        LocalDateTime createdAt,
        String userEmail,
        String deliveryAddress,
        List<OrderItemDto> items,
        PaymentDto payment) {
}
