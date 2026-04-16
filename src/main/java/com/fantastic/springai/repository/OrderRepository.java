package com.fantastic.springai.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fantastic.springai.model.OrderStatus;
import com.fantastic.springai.model.ShopOrder;

public interface OrderRepository extends JpaRepository<ShopOrder, Long> {

    /**
     * JPQL : charge commande + lignes + produits + utilisateur + adresse + paiement en une requête (évite N+1).
     */
    @Query("""
            SELECT DISTINCT o FROM ShopOrder o
            LEFT JOIN FETCH o.orderItems oi
            LEFT JOIN FETCH oi.product
            LEFT JOIN FETCH o.user
            LEFT JOIN FETCH o.address a
            LEFT JOIN FETCH a.country
            LEFT JOIN FETCH o.payment
            WHERE o.id = :id
            """)
    Optional<ShopOrder> findDetailById(@Param("id") Long id);

    List<ShopOrder> findByUser_Id(Long userId);

    List<ShopOrder> findByStatus(OrderStatus status);

    List<ShopOrder> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("""
            SELECT COALESCE(SUM(o.totalAmount), 0)
            FROM ShopOrder o
            WHERE o.createdAt BETWEEN :start AND :end
            """)
    BigDecimal totalRevenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT o.status, COUNT(o) FROM ShopOrder o GROUP BY o.status")
    List<Object[]> countByStatusGrouped();

    @Query("SELECT COALESCE(AVG(o.totalAmount), 0) FROM ShopOrder o")
    BigDecimal avgOrderValue();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM ShopOrder o")
    BigDecimal sumTotalRevenue();

    List<ShopOrder> findByUser_IdAndStatus(Long userId, OrderStatus status);
}
