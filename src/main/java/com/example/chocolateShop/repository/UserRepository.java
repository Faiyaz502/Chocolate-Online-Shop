package com.example.chocolateShop.repository;


import com.example.chocolateShop.entity.User;
import com.example.chocolateShop.enums.Role;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRole(Role role);

    /** Admin customer listing, keyset pagination (newest first). */
    @Query("SELECT u FROM User u WHERE u.role = :role AND (:cursorId IS NULL OR u.id < :cursorId) ORDER BY u.id DESC")
    List<User> findByRoleKeyset(@Param("role") Role role, @Param("cursorId") Long cursorId, Pageable pageable);
}
