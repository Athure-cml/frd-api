package com.furuiduo.quote.cost.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.furuiduo.quote.cost.entity.CostFumigation;

public interface CostFumigationRepository extends JpaRepository<CostFumigation, Long> {

  @Query(
      """
      SELECT f FROM CostFumigation f WHERE
      (:pod IS NULL OR :pod = '' OR UPPER(TRIM(f.port)) = UPPER(TRIM(:pod)))
      ORDER BY f.updatedAt DESC
      """)
  List<CostFumigation> matchByPort(@Param("pod") String pod);
}
