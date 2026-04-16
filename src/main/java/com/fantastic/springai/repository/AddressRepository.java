package com.fantastic.springai.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fantastic.springai.model.Address;

public interface AddressRepository extends JpaRepository<Address, Long> {
}
