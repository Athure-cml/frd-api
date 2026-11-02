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
      f.archived = false
      AND (:station = '' OR UPPER(TRIM(f.station)) = UPPER(:station))
      ORDER BY f.updatedAt DESC
      """)
  List<CostFumigation> matchByStation(@Param("station") String station);

  /**
   * 熏蒸点下拉选项来源：成本库中未归档、有站点名称的记录去重。
   * 归档（archived=true）多为历史/迁移数据，不应再出现在报价单、询价单的可选列表中。
   */
  @Query(
      value =
          """
          SELECT DISTINCT TRIM(station) AS station
          FROM cost_fumigation
          WHERE archived = false
            AND station IS NOT NULL
            AND TRIM(station) <> ''
          ORDER BY station
          """,
      nativeQuery = true)
  List<String> findDistinctStations();

  @Query(
      """
      SELECT f FROM CostFumigation f WHERE
      f.archived = false
      AND (:region = '' OR cast(function('regexp_like', COALESCE(f.region, ''), :region, 'i') as boolean))
      AND (:station = '' OR cast(function('regexp_like', COALESCE(f.station, ''), :station, 'i') as boolean))
      AND (:restrictIds = false OR f.id IN :ids)
      """)
  Page<CostFumigation> search(
      @Param("region") String region,
      @Param("station") String station,
      @Param("restrictIds") boolean restrictIds,
      @Param("ids") List<Long> ids,
      Pageable pageable);

  long countByIdIn(Collection<Long> ids);

  @Query("SELECT f.id FROM CostFumigation f")
  List<Long> findAllIds();
}
