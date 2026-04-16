package com.fantastic.springai.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fantastic.springai.model.Country;

public interface CountryRepository extends JpaRepository<Country, Integer> {
}
