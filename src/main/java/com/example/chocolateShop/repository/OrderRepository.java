package com.example.chocolateShop.repository;

import com.example.chocolateShop.entity.Order;
import com.example.chocolateShop.entity.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    boolean existsByOrderNumber(String orderNumber);




    /** Admin listing, keyset pagination (newest first), optionally filtered by status. */
    @Query("SELECT o FROM Order o " +
            "WHERE (:status IS NULL OR o.status = :status) " +
            "AND (:cursorId IS NULL OR o.id < :cursorId) " +
            "ORDER BY o.id DESC")
    List<Order> findAdminOrdersKeyset(@Param("status") OrderStatus status,
                                      @Param("cursorId") Long cursorId,
                                      Pageable pageable);

    @Query("""
       SELECT COALESCE(SUM(o.grandTotal), 0)
       FROM Order o
       WHERE o.status <> :status
       """)
    BigDecimal sumRevenueExcludingCancelled(@Param("status") OrderStatus status);

    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countGroupedByStatus();



    /** Guest order history/detail lookup, by the IP address the order was placed from. */
    List<Order> findByIpAddressOrderByCreatedAtDesc(String ipAddress);

    Optional<Order> findByIdAndIpAddress(Long id, String ipAddress);
}
