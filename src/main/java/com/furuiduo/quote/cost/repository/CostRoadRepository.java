package com.furuiduo.quote.cost.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.furuiduo.quote.cost.entity.CostRoad;

public interface CostRoadRepository extends JpaRepository<CostRoad, Long> {

  @Query(
      """
      SELECT r FROM CostRoad r WHERE
      (:city IS NULL OR :city = '' OR UPPER(TRIM(r.city)) = UPPER(TRIM(:city)))
      AND (:state IS NULL OR :state = '' OR UPPER(TRIM(r.state)) = UPPER(TRIM(:state)))
      AND (:por IS NULL OR :por = '' OR UPPER(TRIM(r.por)) = UPPER(TRIM(:por)))
      AND (:pol IS NULL OR :pol = '' OR UPPER(TRIM(r.pol)) = UPPER(TRIM(:pol)))
      ORDER BY r.updatedAt DESC
      """)
  List<CostRoad> matchByRoute(
      @Param("city") String city,
      @Param("state") String state,
      @Param("por") String por,
      @Param("pol") String pol);
}
