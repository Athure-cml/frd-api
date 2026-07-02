package com.furuiduo.quote.cost.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.furuiduo.quote.cost.entity.CostSea;

public interface CostSeaRepository extends JpaRepository<CostSea, Long> {

  @Query(
      """
      SELECT s FROM CostSea s WHERE
      (:pol IS NULL OR :pol = '' OR UPPER(TRIM(s.origin)) = UPPER(TRIM(:pol)))
      AND (:pod IS NULL OR :pod = '' OR UPPER(TRIM(s.destination)) = UPPER(TRIM(:pod)))
      AND (:ssl IS NULL OR :ssl = '' OR UPPER(TRIM(s.carrier)) = UPPER(TRIM(:ssl)))
      ORDER BY s.updatedAt DESC
      """)
  List<CostSea> matchByRoute(
      @Param("pol") String pol, @Param("pod") String pod, @Param("ssl") String ssl);
}
