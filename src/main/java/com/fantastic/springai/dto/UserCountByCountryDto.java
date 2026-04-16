package com.fantastic.springai.dto;

public record UserCountByCountryDto(
        Integer countryId,
        String countryCode,
        String countryName,
        Long userCount) {
}
