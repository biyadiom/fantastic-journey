package com.fantastic.springai.dto;

public record UserSummaryDto(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        boolean active,
        boolean seller) {
}
