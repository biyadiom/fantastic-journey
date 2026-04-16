package com.fantastic.springai.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fantastic.springai.model.PaymentMethod;
import com.fantastic.springai.model.PaymentStatus;

public record PaymentDto(
        PaymentMethod method,
        PaymentStatus status,
        BigDecimal amount,
        LocalDateTime paidAt,
        String transactionRef) {
}
