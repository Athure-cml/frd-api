package com.furuiduo.quote.cost.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.furuiduo.quote.cost.entity.CostSea;

public interface CostSeaRepository extends JpaRepository<CostSea, Long> {

  @Query(
      """
      SELECT s FROM CostSea s WHERE
      (:pol = '' OR UPPER(TRIM(s.pol)) = UPPER(:pol))
      AND (:pod = '' OR UPPER(TRIM(s.pod)) = UPPER(:pod))
      AND (:ssl = '' OR UPPER(TRIM(s.ssl)) = UPPER(:ssl))
      ORDER BY s.updatedAt DESC
      """)
  List<CostSea> matchByRoute(
      @Param("pol") String pol,
      @Param("pod") String pod,
      @Param("ssl") String ssl);

  @Query(
      """
      SELECT s FROM CostSea s WHERE
      (:por = '' OR LOWER(COALESCE(s.por, '')) LIKE LOWER(CONCAT('%', :por, '%')))
      AND (:pol = '' OR LOWER(COALESCE(s.pol, '')) LIKE LOWER(CONCAT('%', :pol, '%')))
      AND (:pod = '' OR LOWER(COALESCE(s.pod, '')) LIKE LOWER(CONCAT('%', :pod, '%')))
      AND (:ssl = '' OR LOWER(COALESCE(s.ssl, '')) LIKE LOWER(CONCAT('%', :ssl, '%')))
      AND (:containerType = '' OR LOWER(COALESCE(s.containerType, '')) LIKE LOWER(CONCAT('%', :containerType, '%')))
      AND (:agent = '' OR LOWER(COALESCE(s.agent, '')) LIKE LOWER(CONCAT('%', :agent, '%')))
      """)
  Page<CostSea> search(
      @Param("por") String por,
      @Param("pol") String pol,
      @Param("pod") String pod,
      @Param("ssl") String ssl,
      @Param("containerType") String containerType,
      @Param("agent") String agent,
      Pageable pageable);
}
