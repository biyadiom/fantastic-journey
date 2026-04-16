package com.fantastic.springai.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fantastic.springai.model.Seller;

public interface SellerRepository extends JpaRepository<Seller, Long> {

    List<Seller> findByVerifiedTrue();

    List<Seller> findByStoreNameContainingIgnoreCase(String name);

    Optional<Seller> findByStoreNameIgnoreCase(String storeName);

    @Query("SELECT s FROM Seller s JOIN s.user u WHERE LOWER(u.email) = LOWER(:email)")
    Optional<Seller> findByUserEmail(@Param("email") String email);

    @Query("SELECT s FROM Seller s ORDER BY s.totalSales DESC")
    Page<Seller> findTopSellers(Pageable pageable);
}
