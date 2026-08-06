package com.furuiduo.quote.shippingline.repository;

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
      AND (:name = '' OR UPPER(s.name) LIKE UPPER(CONCAT('%', :name, '%')))
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
}
