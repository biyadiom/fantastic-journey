package com.fantastic.springai.dto;

import java.math.BigDecimal;

public record SellerStatsDto(
        Long sellerId,
        String storeName,
        Integer totalSales,
        BigDecimal rating,
        boolean verified,
        long productCount) {
}
