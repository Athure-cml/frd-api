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
      (:city = '' OR UPPER(TRIM(r.city)) = UPPER(:city))
      AND (:state = '' OR UPPER(TRIM(r.state)) = UPPER(:state))
      AND (:por = '' OR UPPER(TRIM(r.por)) = UPPER(:por))
      AND (:pol = '' OR UPPER(TRIM(r.pol)) = UPPER(:pol))
      ORDER BY r.updatedAt DESC
      """)
  List<CostRoad> matchByRoute(
      @Param("city") String city,
      @Param("state") String state,
      @Param("por") String por,
      @Param("pol") String pol);
}
