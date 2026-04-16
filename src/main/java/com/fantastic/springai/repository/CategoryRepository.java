package com.fantastic.springai.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.fantastic.springai.model.Category;

public interface CategoryRepository extends JpaRepository<Category, Integer> {

    /**
     * JPQL : compte produits par catégorie et CA agrégé sur order_items (quantité × prix unitaire).
     */
    @Query("""
            SELECT c.id, c.name, COUNT(DISTINCT p.id), COALESCE(SUM(oi.quantity * oi.unitPrice), 0)
            FROM Category c
            LEFT JOIN c.products p
            LEFT JOIN p.orderItems oi
            GROUP BY c.id, c.name
            ORDER BY c.name
            """)
    List<Object[]> findCategoryAggregation();

    List<Category> findByParentIsNull();

    List<Category> findByParent_Id(Integer parentId);

    Optional<Category> findBySlug(String slug);
}
