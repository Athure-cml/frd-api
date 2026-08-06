package com.furuiduo.quote.supplier.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

import com.furuiduo.quote.supplier.entity.Supplier;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

  boolean existsByCode(String code);

  Optional<Supplier> findByCode(String code);

  @Query(
      """
      SELECT s FROM Supplier s
      WHERE LOWER(TRIM(s.name)) = LOWER(TRIM(:name))
      """)
  Optional<Supplier> findByNameNormalized(@Param("name") String name);

  @Query(
      """
      SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Supplier s
      WHERE LOWER(TRIM(s.name)) = LOWER(TRIM(:name))
      AND (:excludeId IS NULL OR s.id <> :excludeId)
      """)
  boolean existsByNameNormalized(
      @Param("name") String name, @Param("excludeId") Long excludeId);

  @Query(
      """
      SELECT s FROM Supplier s WHERE
      (:code = '' OR UPPER(s.code) LIKE UPPER(CONCAT('%', :code, '%')))
      AND (:name = '' OR UPPER(s.name) LIKE UPPER(CONCAT('%', :name, '%')))
      AND (:status IS NULL OR s.status = :status)
      """)
  Page<Supplier> search(
      @Param("code") String code,
      @Param("name") String name,
      @Param("status") Integer status,
      Pageable pageable);

  @Query("SELECT s.code FROM Supplier s WHERE s.code LIKE :prefix ORDER BY s.code DESC")
  Page<String> findSupplierCodesByPrefix(@Param("prefix") String prefix, Pageable pageable);

  Optional<Supplier> findFirstByNameIgnoreCase(String name);
}
