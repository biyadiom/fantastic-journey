package com.fantastic.springai.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.fantastic.springai.model.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategory_Id(Integer categoryId);

    List<Product> findBySeller_Id(Long sellerId);

    @EntityGraph(attributePaths = { "category", "seller" })
    Page<Product> findByActiveTrue(Pageable pageable);

    List<Product> findByStockLessThan(int threshold);

    List<Product> findByBasePriceBetweenAndActiveTrue(BigDecimal min, BigDecimal max);

    /**
     * JPQL : agrège les quantités vendues par produit via order_items ; tri par volume décroissant.
     */
    @Query("""
            SELECT oi.product FROM OrderItem oi
            GROUP BY oi.product
            ORDER BY SUM(oi.quantity) DESC
            """)
    List<Product> findTopSellingProducts(Pageable pageable);

    List<Product> findByNameContainingIgnoreCase(String name);
}
