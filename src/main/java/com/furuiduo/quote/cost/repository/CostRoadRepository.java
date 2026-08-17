package com.furuiduo.quote.cost.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.furuiduo.quote.cost.entity.CostRoad;

public interface CostRoadRepository extends JpaRepository<CostRoad, Long> {

  @Query(
      """
      SELECT r FROM CostRoad r WHERE
      (:zipCode = '' OR UPPER(TRIM(r.zipCode)) = UPPER(:zipCode))
      AND (:city = '' OR UPPER(TRIM(r.city)) = UPPER(:city))
      AND (:state = '' OR UPPER(TRIM(r.state)) = UPPER(:state))
      AND (:por = '' OR UPPER(TRIM(r.por)) = UPPER(:por))
      AND (:pol = '' OR UPPER(TRIM(r.pol)) = UPPER(:pol))
      AND (:supplier = '' OR UPPER(TRIM(r.supplier)) = UPPER(:supplier))
      ORDER BY r.updatedAt DESC
      """)
  List<CostRoad> matchByRoute(
      @Param("zipCode") String zipCode,
      @Param("city") String city,
      @Param("state") String state,
      @Param("por") String por,
      @Param("pol") String pol,
      @Param("supplier") String supplier);

  @Query(
      """
      SELECT r FROM CostRoad r WHERE
      (:zipCode = '' OR LOWER(COALESCE(r.zipCode, '')) LIKE LOWER(CONCAT('%', :zipCode, '%')))
      AND (:city = '' OR LOWER(COALESCE(r.city, '')) LIKE LOWER(CONCAT('%', :city, '%')))
      AND (:state = '' OR LOWER(COALESCE(r.state, '')) LIKE LOWER(CONCAT('%', :state, '%')))
      AND (:por = '' OR LOWER(COALESCE(r.por, '')) LIKE LOWER(CONCAT('%', :por, '%')))
      AND (:pol = '' OR LOWER(COALESCE(r.pol, '')) LIKE LOWER(CONCAT('%', :pol, '%')))
      AND (:supplier = '' OR LOWER(COALESCE(r.supplier, '')) LIKE LOWER(CONCAT('%', :supplier, '%')))
      AND (:redelivery IS NULL OR r.redelivery = :redelivery)
      AND (:validDate = '' OR LOWER(COALESCE(r.validDate, '')) LIKE LOWER(CONCAT('%', :validDate, '%')))
      """)
  Page<CostRoad> search(
      @Param("zipCode") String zipCode,
      @Param("city") String city,
      @Param("state") String state,
      @Param("por") String por,
      @Param("pol") String pol,
      @Param("supplier") String supplier,
      @Param("redelivery") BigDecimal redelivery,
      @Param("validDate") String validDate,
      Pageable pageable);
}
