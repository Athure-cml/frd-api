package com.furuiduo.quote.supplier.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.furuiduo.quote.supplier.entity.SupplierType;

public interface SupplierTypeRepository extends JpaRepository<SupplierType, Long> {

  List<SupplierType> findAllByOrderBySortOrderAscIdAsc();

  List<SupplierType> findByStatusOrderBySortOrderAscIdAsc(Integer status);

  @Query(
      """
      SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM SupplierType t
      WHERE LOWER(TRIM(t.name)) = LOWER(TRIM(:name))
      AND (:excludeId IS NULL OR t.id <> :excludeId)
      """)
  boolean existsByNameNormalized(
      @Param("name") String name, @Param("excludeId") Long excludeId);

  Optional<SupplierType> findByNameIgnoreCase(String name);
}
