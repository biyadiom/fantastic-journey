package com.fantastic.springai.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fantastic.springai.model.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProduct_Id(Long productId);

    List<Review> findByUser_Id(Long userId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
    Double avgRatingByProductId(@Param("productId") Long productId);

    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.product.id = :productId GROUP BY r.rating ORDER BY r.rating")
    List<Object[]> countByRatingGrouped(@Param("productId") Long productId);
}
