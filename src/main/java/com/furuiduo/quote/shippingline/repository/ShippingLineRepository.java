package com.furuiduo.quote.shippingline.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.furuiduo.quote.shippingline.entity.ShippingLine;

public interface ShippingLineRepository extends JpaRepository<ShippingLine, Long> {

  boolean existsByCode(String code);

  Optional<ShippingLine> findByCode(String code);

  @Query(
      """
      SELECT s FROM ShippingLine s
      WHERE LOWER(TRIM(s.name)) = LOWER(TRIM(:name))
      """)
  Optional<ShippingLine> findByNameNormalized(@Param("name") String name);

  @Query(
      """
      SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM ShippingLine s
      WHERE LOWER(TRIM(s.name)) = LOWER(TRIM(:name))
      AND (:excludeId IS NULL OR s.id <> :excludeId)
      """)
  boolean existsByNameNormalized(
      @Param("name") String name, @Param("excludeId") Long excludeId);

  @Query(
      """
      SELECT s FROM ShippingLine s WHERE
      (:code = '' OR UPPER(s.code) LIKE UPPER(CONCAT('%', :code, '%')))
      AND (
        :name = ''
        OR UPPER(s.name) LIKE UPPER(CONCAT('%', :name, '%'))
        OR (s.shortName IS NOT NULL AND UPPER(s.shortName) LIKE UPPER(CONCAT('%', :name, '%')))
      )
      AND (:status IS NULL OR s.status = :status)
      """)
  Page<ShippingLine> search(
      @Param("code") String code,
      @Param("name") String name,
      @Param("status") Integer status,
      Pageable pageable);

  @Query("SELECT s.code FROM ShippingLine s WHERE s.code LIKE :prefix ORDER BY s.code DESC")
  Page<String> findCodesByPrefix(@Param("prefix") String prefix, Pageable pageable);

  Optional<ShippingLine> findFirstByNameIgnoreCase(String name);

  @Query(
      """
      SELECT s FROM ShippingLine s
      WHERE s.shortName IS NOT NULL
      AND TRIM(s.shortName) <> ''
      AND LOWER(TRIM(s.shortName)) = LOWER(TRIM(:shortName))
      """)
  Optional<ShippingLine> findFirstByShortNameNormalized(@Param("shortName") String shortName);

  default Optional<ShippingLine> findByNameOrShortName(String key) {
    if (key == null || key.isBlank()) {
      return Optional.empty();
    }
    String trimmed = key.trim();
    return findByNameNormalized(trimmed).or(() -> findFirstByShortNameNormalized(trimmed));
  }

  @Query(
      "SELECT s FROM ShippingLine s WHERE (:pinned = true AND s.pinnedAt IS NOT NULL) OR (:pinned = false AND s.pinnedAt IS NULL) ORDER BY s.sortOrder ASC, s.updatedAt DESC")
  List<ShippingLine> findAllByPinned(@Param("pinned") boolean pinned);

  @Query("SELECT COALESCE(MIN(s.sortOrder), 0) FROM ShippingLine s WHERE s.pinnedAt IS NOT NULL")
  Integer minPinnedSortOrder();

  @Query("SELECT COALESCE(MAX(s.sortOrder), 0) FROM ShippingLine s WHERE s.pinnedAt IS NULL")
  Integer maxUnpinnedSortOrder();
}
