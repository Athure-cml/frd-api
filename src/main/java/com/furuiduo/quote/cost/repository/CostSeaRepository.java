package com.furuiduo.quote.cost.repository;

import java.util.Collection;
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
      (:por = '' OR UPPER(TRIM(s.por)) = UPPER(:por))
      AND (:pod = '' OR UPPER(TRIM(s.pod)) = UPPER(:pod))
      AND (:ssl = '' OR UPPER(TRIM(s.ssl)) = UPPER(:ssl))
      ORDER BY s.updatedAt DESC
      """)
  List<CostSea> matchByRoute(
      @Param("por") String por,
      @Param("pod") String pod,
      @Param("ssl") String ssl);

  /**
   * 列表/导出搜索：por、pol、pod、ssl、containerType、agent 支持多选，
   * 参数为「值1|值2」形式，服务层转成正则后由 regexp_like 做 OR 包含匹配
   * （pol 等字段存 "A/B" 并联值，必须用包含匹配而非 IN）。
   */
  @Query(
      """
      SELECT s FROM CostSea s WHERE
      (:por = '' OR cast(function('regexp_like', COALESCE(s.por, ''), :por, 'i') as boolean))
      AND (:pol = '' OR cast(function('regexp_like', COALESCE(s.pol, ''), :pol, 'i') as boolean))
      AND (:pod = '' OR cast(function('regexp_like', COALESCE(s.pod, ''), :pod, 'i') as boolean))
      AND (:ssl = '' OR cast(function('regexp_like', COALESCE(s.ssl, ''), :ssl, 'i') as boolean))
      AND (:containerType = '' OR cast(function('regexp_like', COALESCE(s.containerType, ''), :containerType, 'i') as boolean))
      AND (:agent = '' OR cast(function('regexp_like', COALESCE(s.agent, ''), :agent, 'i') as boolean))
      AND (:remark = '' OR LOWER(COALESCE(s.remark, '')) LIKE LOWER(CONCAT('%', :remark, '%')))
      AND (:restrictIds = false OR s.id IN :ids)
      """)
  Page<CostSea> search(
      @Param("por") String por,
      @Param("pol") String pol,
      @Param("pod") String pod,
      @Param("ssl") String ssl,
      @Param("containerType") String containerType,
      @Param("agent") String agent,
      @Param("remark") String remark,
      @Param("restrictIds") boolean restrictIds,
      @Param("ids") List<Long> ids,
      Pageable pageable);

  long countByIdIn(Collection<Long> ids);

  @Query("SELECT s.id FROM CostSea s")
  List<Long> findAllIds();
}
