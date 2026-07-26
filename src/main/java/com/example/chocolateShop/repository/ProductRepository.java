package com.example.chocolateShop.repository;


import com.example.chocolateShop.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    long countByActiveTrueAndStockQuantityLessThan(int threshold);

    /**
     * Keyset pagination, newest first (id DESC).
     * The cursor carries only the last-seen id: since ids are monotonically
     * increasing (BIGSERIAL), "id < :cursorId" reliably resumes after it.
     */
    @Query(value = """
            SELECT p.* FROM products p
            WHERE (:activeOnly = false OR p.is_active = true)
              AND (:categoryId IS NULL OR p.category_id = :categoryId)
              AND (:keyword IS NULL OR p.name ILIKE CONCAT('%', :keyword, '%'))
              AND (:minPrice IS NULL OR COALESCE(p.discount_price, p.price) >= :minPrice)
              AND (:maxPrice IS NULL OR COALESCE(p.discount_price, p.price) <= :maxPrice)
              AND (:cursorId IS NULL OR p.id < :cursorId)
            ORDER BY p.id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Product> findNewestKeyset(
            @Param("activeOnly") boolean activeOnly,
            @Param("categoryId") Long categoryId,
            @Param("keyword") String keyword,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("cursorId") Long cursorId,
            @Param("limit") int limit
    );

    /**
     * Keyset pagination, cheapest first.
     * The cursor carries (lastPrice, lastId) so ties on price are broken
     * deterministically by id, which is required for keyset pagination to
     * never skip or repeat a row across pages.
     */
    @Query(value = """
            SELECT p.* FROM products p
            WHERE (:activeOnly = false OR p.is_active = true)
              AND (:categoryId IS NULL OR p.category_id = :categoryId)
              AND (:keyword IS NULL OR p.name ILIKE CONCAT('%', :keyword, '%'))
              AND (:minPrice IS NULL OR COALESCE(p.discount_price, p.price) >= :minPrice)
              AND (:maxPrice IS NULL OR COALESCE(p.discount_price, p.price) <= :maxPrice)
              AND (
                    :cursorId IS NULL
                    OR COALESCE(p.discount_price, p.price) > :cursorPrice
                    OR (COALESCE(p.discount_price, p.price) = :cursorPrice AND p.id > :cursorId)
              )
            ORDER BY COALESCE(p.discount_price, p.price) ASC, p.id ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<Product> findPriceAscKeyset(
            @Param("activeOnly") boolean activeOnly,
            @Param("categoryId") Long categoryId,
            @Param("keyword") String keyword,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("cursorId") Long cursorId,
            @Param("cursorPrice") BigDecimal cursorPrice,
            @Param("limit") int limit
    );

    /** Keyset pagination, most expensive first. Mirror of findPriceAscKeyset. */
    @Query(value = """
            SELECT p.* FROM products p
            WHERE (:activeOnly = false OR p.is_active = true)
              AND (:categoryId IS NULL OR p.category_id = :categoryId)
              AND (:keyword IS NULL OR p.name ILIKE CONCAT('%', :keyword, '%'))
              AND (:minPrice IS NULL OR COALESCE(p.discount_price, p.price) >= :minPrice)
              AND (:maxPrice IS NULL OR COALESCE(p.discount_price, p.price) <= :maxPrice)
              AND (
                    :cursorId IS NULL
                    OR COALESCE(p.discount_price, p.price) < :cursorPrice
                    OR (COALESCE(p.discount_price, p.price) = :cursorPrice AND p.id < :cursorId)
              )
            ORDER BY COALESCE(p.discount_price, p.price) DESC, p.id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Product> findPriceDescKeyset(
            @Param("activeOnly") boolean activeOnly,
            @Param("categoryId") Long categoryId,
            @Param("keyword") String keyword,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("cursorId") Long cursorId,
            @Param("cursorPrice") BigDecimal cursorPrice,
            @Param("limit") int limit
    );
}
