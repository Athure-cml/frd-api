package com.furuiduo.quote.supplier.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.furuiduo.quote.supplier.entity.Supplier;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

  boolean existsByCode(String code);

  Optional<Supplier> findByCode(String code);

  @Query(
      """
      SELECT s FROM Supplier s
      WHERE s.category = :category
      AND LOWER(TRIM(s.name)) = LOWER(TRIM(:name))
      """)
  Optional<Supplier> findByCategoryAndNameNormalized(
      @Param("category") String category, @Param("name") String name);

  @Query(
      """
      SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Supplier s
      WHERE s.category = :category
      AND LOWER(TRIM(s.name)) = LOWER(TRIM(:name))
      AND (:excludeId IS NULL OR s.id <> :excludeId)
      """)
  boolean existsByCategoryAndNameNormalized(
      @Param("category") String category,
      @Param("name") String name,
      @Param("excludeId") Long excludeId);

  @Query(
      value =
          """
          SELECT * FROM supplier s WHERE
          (CAST(:category AS text) = '' OR s.category = CAST(:category AS text))
          AND (CAST(:code AS text) = '' OR UPPER(s.code) LIKE UPPER(CONCAT('%', CAST(:code AS text), '%')))
          AND (
            CAST(:name AS text) = ''
            OR UPPER(s.name) LIKE UPPER(CONCAT('%', CAST(:name AS text), '%'))
            OR (
              s.short_name IS NOT NULL
              AND UPPER(s.short_name) LIKE UPPER(CONCAT('%', CAST(:name AS text), '%'))
            )
          )
          AND (CAST(:status AS integer) IS NULL OR s.status = CAST(:status AS integer))
          AND (
            CAST(:typeId AS text) = ''
            OR s.types @> CAST(CONCAT('["', CAST(:typeId AS text), '"]') AS jsonb)
          )
          ORDER BY CASE WHEN s.pinned_at IS NULL THEN 1 ELSE 0 END ASC,
                   s.sort_order ASC,
                   s.updated_at DESC
          """,
      countQuery =
          """
          SELECT COUNT(*) FROM supplier s WHERE
          (CAST(:category AS text) = '' OR s.category = CAST(:category AS text))
          AND (CAST(:code AS text) = '' OR UPPER(s.code) LIKE UPPER(CONCAT('%', CAST(:code AS text), '%')))
          AND (
            CAST(:name AS text) = ''
            OR UPPER(s.name) LIKE UPPER(CONCAT('%', CAST(:name AS text), '%'))
            OR (
              s.short_name IS NOT NULL
              AND UPPER(s.short_name) LIKE UPPER(CONCAT('%', CAST(:name AS text), '%'))
            )
          )
          AND (CAST(:status AS integer) IS NULL OR s.status = CAST(:status AS integer))
          AND (
            CAST(:typeId AS text) = ''
            OR s.types @> CAST(CONCAT('["', CAST(:typeId AS text), '"]') AS jsonb)
          )
          """,
      nativeQuery = true)
  Page<Supplier> search(
      @Param("category") String category,
      @Param("code") String code,
      @Param("name") String name,
      @Param("status") Integer status,
      @Param("typeId") String typeId,
      Pageable pageable);

  @Query("SELECT s.code FROM Supplier s WHERE s.code LIKE :prefix ORDER BY s.code DESC")
  Page<String> findSupplierCodesByPrefix(@Param("prefix") String prefix, Pageable pageable);

  Optional<Supplier> findFirstByCategoryAndNameIgnoreCase(String category, String name);

  Optional<Supplier> findFirstByNameIgnoreCase(String name);

  @Query(
      """
      SELECT s FROM Supplier s
      WHERE s.category = :category
      AND s.shortName IS NOT NULL
      AND TRIM(s.shortName) <> ''
      AND LOWER(TRIM(s.shortName)) = LOWER(TRIM(:shortName))
      """)
  Optional<Supplier> findFirstByCategoryAndShortNameNormalized(
      @Param("category") String category, @Param("shortName") String shortName);

  default Optional<Supplier> findByCategoryAndNameOrShortName(
      String category, String key) {
    if (key == null || key.isBlank()) {
      return Optional.empty();
    }
    String trimmed = key.trim();
    return findByCategoryAndNameNormalized(category, trimmed)
        .or(() -> findFirstByCategoryAndShortNameNormalized(category, trimmed));
  }

  @Query(
      value =
          """
          SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END
          FROM supplier s
          WHERE s.category = 'OTHER'
            AND s.types @> CAST(:typeJson AS jsonb)
          """,
      nativeQuery = true)
  boolean existsOtherSupplierUsingType(@Param("typeJson") String typeJson);

  @Query(
      "SELECT s FROM Supplier s WHERE s.category = :category AND ((:pinned = true AND s.pinnedAt IS NOT NULL) OR (:pinned = false AND s.pinnedAt IS NULL)) ORDER BY s.sortOrder ASC, s.updatedAt DESC")
  List<Supplier> findAllByCategoryAndPinned(
      @Param("category") String category, @Param("pinned") boolean pinned);

  @Query(
      "SELECT COALESCE(MIN(s.sortOrder), 0) FROM Supplier s WHERE s.category = :category AND s.pinnedAt IS NOT NULL")
  Integer minPinnedSortOrder(@Param("category") String category);

  @Query(
      "SELECT COALESCE(MAX(s.sortOrder), 0) FROM Supplier s WHERE s.category = :category AND s.pinnedAt IS NULL")
  Integer maxUnpinnedSortOrder(@Param("category") String category);
}
