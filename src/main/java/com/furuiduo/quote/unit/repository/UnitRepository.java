package com.furuiduo.quote.unit.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.furuiduo.quote.unit.entity.Unit;

public interface UnitRepository extends JpaRepository<Unit, Long> {

  boolean existsByCode(String code);

  Optional<Unit> findByCode(String code);

  List<Unit> findByStatusOrderBySortAscCodeAsc(Integer status);

  @Query(
      """
      SELECT u FROM Unit u
      WHERE (:code = '' OR UPPER(u.code) LIKE UPPER(CONCAT('%', :code, '%')))
        AND (:name = '' OR UPPER(u.name) LIKE UPPER(CONCAT('%', :name, '%')))
        AND (:status IS NULL OR u.status = :status)
      ORDER BY u.sort ASC, u.code ASC
      """)
  List<Unit> search(
      @Param("code") String code, @Param("name") String name, @Param("status") Integer status);

  @Query(
      """
      SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM Unit u
      WHERE LOWER(TRIM(u.code)) = LOWER(TRIM(:code))
        AND (:excludeId IS NULL OR u.id <> :excludeId)
      """)
  boolean existsByCodeNormalized(@Param("code") String code, @Param("excludeId") Long excludeId);
}
