package com.furuiduo.quote.cost.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.furuiduo.quote.cost.entity.CostFumigation;

public interface CostFumigationRepository extends JpaRepository<CostFumigation, Long> {

  @Query(
      """
      SELECT f FROM CostFumigation f WHERE
      (:pod = '' OR UPPER(TRIM(f.region)) = UPPER(:pod))
      ORDER BY f.updatedAt DESC
      """)
  List<CostFumigation> matchByPort(@Param("pod") String pod);

  @Query(
      """
      SELECT f FROM CostFumigation f WHERE
      (:region = '' OR LOWER(COALESCE(f.region, '')) LIKE LOWER(CONCAT('%', :region, '%')))
      AND (:station = '' OR LOWER(COALESCE(f.station, '')) LIKE LOWER(CONCAT('%', :station, '%')))
      AND (:outdoorValidity = '' OR LOWER(COALESCE(f.outdoorValidity, '')) LIKE LOWER(CONCAT('%', :outdoorValidity, '%')))
      AND (:indoorValidity = '' OR LOWER(COALESCE(f.indoorValidity, '')) LIKE LOWER(CONCAT('%', :indoorValidity, '%')))
      """)
  Page<CostFumigation> search(
      @Param("region") String region,
      @Param("station") String station,
      @Param("outdoorValidity") String outdoorValidity,
      @Param("indoorValidity") String indoorValidity,
      Pageable pageable);
}
