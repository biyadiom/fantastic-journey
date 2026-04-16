package com.fantastic.springai.dto;

import java.math.BigDecimal;

import com.fantastic.springai.model.PaymentMethod;

public record PaymentStatsDto(
        PaymentMethod method,
        long count,
        BigDecimal totalAmount,
        double completionRate) {
}
