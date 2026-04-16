package com.fantastic.springai.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fantastic.springai.model.Payment;
import com.fantastic.springai.model.PaymentMethod;
import com.fantastic.springai.model.PaymentStatus;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrder_Id(Long orderId);

    List<Payment> findByStatus(PaymentStatus status);

    List<Payment> findByMethod(PaymentMethod method);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = :st")
    BigDecimal totalCompletedRevenue(@Param("st") PaymentStatus st);

    /**
     * JPQL : par méthode de paiement — total lignes, montant cumulé, nombre de paiements complétés.
     */
    @Query("""
            SELECT p.method, COUNT(p), COALESCE(SUM(p.amount), 0),
                   SUM(CASE WHEN p.status = :done THEN 1 ELSE 0 END)
            FROM Payment p
            GROUP BY p.method
            ORDER BY p.method
            """)
    List<Object[]> aggregateByMethod(@Param("done") PaymentStatus done);
}
