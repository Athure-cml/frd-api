package com.furuiduo.quote.cost.repository;

import java.util.Collection;
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
      (:station = '' OR UPPER(TRIM(f.station)) = UPPER(:station))
      ORDER BY f.updatedAt DESC
      """)
  List<CostFumigation> matchByStation(@Param("station") String station);

  @Query(
      value =
          """
          SELECT DISTINCT TRIM(station) AS station
          FROM cost_fumigation
          WHERE station IS NOT NULL AND TRIM(station) <> ''
          ORDER BY station
          """,
      nativeQuery = true)
  List<String> findDistinctStations();

  @Query(
      """
      SELECT f FROM CostFumigation f WHERE
      (:region = '' OR LOWER(COALESCE(f.region, '')) LIKE LOWER(CONCAT('%', :region, '%')))
      AND (:station = '' OR LOWER(COALESCE(f.station, '')) LIKE LOWER(CONCAT('%', :station, '%')))
      AND (:restrictIds = false OR f.id IN :ids)
      """)
  Page<CostFumigation> search(
      @Param("region") String region,
      @Param("station") String station,
      @Param("restrictIds") boolean restrictIds,
      @Param("ids") List<Long> ids,
      Pageable pageable);

  long countByIdIn(Collection<Long> ids);
}
