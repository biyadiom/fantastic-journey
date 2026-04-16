package com.fantastic.springai.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fantastic.springai.dto.UserCountByCountryDto;
import com.fantastic.springai.dto.UserOrderStatsDto;
import com.fantastic.springai.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByUsernameIgnoreCase(String username);

    List<User> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(String firstName, String lastName);

    /**
     * JPQL implicite : même motif sur email, username, prénom et nom (OR).
     */
    Page<User> findByEmailContainingIgnoreCaseOrUsernameContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String email,
            String username,
            String firstName,
            String lastName,
            Pageable pageable);

    Page<User> findByActiveTrue(Pageable pageable);

    List<User> findBySellerTrue();

    long countByCountry_Id(Integer countryId);

    /**
     * JPQL : jointure {@code Country} / {@code User}, {@code COUNT} par pays (0 si aucun utilisateur lié).
     */
    @Query("""
            SELECT new com.fantastic.springai.dto.UserCountByCountryDto(c.id, c.code, c.name, COUNT(u))
            FROM Country c LEFT JOIN c.users u
            GROUP BY c.id, c.code, c.name
            ORDER BY c.name
            """)
    List<UserCountByCountryDto> countUsersByCountry();

    long countByActiveTrue();

    long countBySellerTrue();

    @Query("""
            SELECT new com.fantastic.springai.dto.UserOrderStatsDto(
                u.id, u.username, COUNT(o), COALESCE(SUM(o.totalAmount), 0), MAX(o.createdAt))
            FROM User u LEFT JOIN u.orders o
            GROUP BY u.id, u.username
            ORDER BY COALESCE(SUM(o.totalAmount), 0) DESC
            """)
    List<UserOrderStatsDto> findTopSpenders(Pageable pageable);
}
