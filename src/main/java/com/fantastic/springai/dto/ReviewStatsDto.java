package com.fantastic.springai.dto;

import java.util.Map;

public record ReviewStatsDto(
        Long productId,
        String productName,
        Double avgRating,
        long totalReviews,
        Map<Short, Long> ratingDistribution) {
}
