package com.furuiduo.quote.masterdata.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.furuiduo.quote.masterdata.entity.MdGlobalPort;
import com.furuiduo.quote.masterdata.entity.PortType;

public interface MdGlobalPortRepository extends JpaRepository<MdGlobalPort, Long> {

  boolean existsByCode(String code);

  Optional<MdGlobalPort> findByCode(String code);

  @Query(
      """
      SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
      FROM MdGlobalPort p
      WHERE UPPER(TRIM(p.nameEn)) = UPPER(TRIM(:nameEn))
        AND (:restrictTypes = false OR p.portType IN :portTypes)
      """)
  boolean existsByNameEnIgnoreCase(
      @Param("nameEn") String nameEn,
      @Param("portTypes") Collection<PortType> portTypes,
      @Param("restrictTypes") boolean restrictTypes);

  @Query(
      """
      SELECT p FROM MdGlobalPort p
      WHERE UPPER(p.nameEn) = UPPER(:nameEn)
        AND (
          (:countryRegion = '' AND (p.countryRegion IS NULL OR TRIM(p.countryRegion) = ''))
          OR UPPER(COALESCE(p.countryRegion, '')) = UPPER(:countryRegion)
        )
        AND p.portType = :portType
      """)
  Optional<MdGlobalPort> findByBusinessKey(
      @Param("nameEn") String nameEn,
      @Param("countryRegion") String countryRegion,
      @Param("portType") PortType portType);

  List<MdGlobalPort> findByCodeIn(Collection<String> codes);

  @Query("SELECT UPPER(p.code) FROM MdGlobalPort p")
  List<String> findAllCodes();

  @Query(
      """
      SELECT p FROM MdGlobalPort p
      WHERE (:code = '' OR UPPER(p.code) LIKE UPPER(CONCAT('%', :code, '%')))
        AND (:nameEn = '' OR UPPER(p.nameEn) LIKE UPPER(CONCAT('%', :nameEn, '%')))
        AND (:nameZh = '' OR p.nameZh LIKE CONCAT('%', :nameZh, '%'))
        AND (:route = '' OR UPPER(p.route) LIKE UPPER(CONCAT('%', :route, '%')))
        AND (:countryRegion = '' OR UPPER(p.countryRegion) LIKE UPPER(CONCAT('%', :countryRegion, '%')))
        AND (:portType IS NULL OR p.portType = :portType)
      ORDER BY p.code ASC
      """)
  List<MdGlobalPort> search(
      @Param("code") String code,
      @Param("nameEn") String nameEn,
      @Param("nameZh") String nameZh,
      @Param("route") String route,
      @Param("countryRegion") String countryRegion,
      @Param("portType") PortType portType);

  /**
   * 下拉搜索：无关键词时优先返回已有中文名的港口；有关键词时按编码/英文/中文模糊匹配，
   * 并按「精确 → 前缀 → 词边界 → 包含」排序，避免 Bannewitz 这类中间命中抢在 New York 前面。
   */
  @Query(
      """
      SELECT p FROM MdGlobalPort p
      WHERE (:portTypesEmpty = true OR p.portType IN :portTypes)
        AND (
          (:keyword = '' AND p.nameZh IS NOT NULL AND TRIM(p.nameZh) <> '')
          OR (
            :keyword <> ''
            AND (
              UPPER(p.code) LIKE UPPER(CONCAT('%', :keyword, '%'))
              OR UPPER(p.nameEn) LIKE UPPER(CONCAT('%', :keyword, '%'))
              OR (p.nameZh IS NOT NULL AND p.nameZh LIKE CONCAT('%', :keyword, '%'))
            )
          )
        )
      ORDER BY
        CASE
          WHEN :keyword <> '' AND UPPER(p.code) = UPPER(:keyword) THEN 0
          WHEN :keyword <> '' AND UPPER(p.nameEn) = UPPER(:keyword) THEN 1
          WHEN :keyword <> '' AND p.nameZh IS NOT NULL AND UPPER(TRIM(p.nameZh)) = UPPER(:keyword) THEN 2
          WHEN :keyword <> '' AND UPPER(p.code) LIKE UPPER(CONCAT(:keyword, '%')) THEN 3
          WHEN :keyword <> '' AND UPPER(p.nameEn) LIKE UPPER(CONCAT(:keyword, '%')) THEN 4
          WHEN :keyword <> '' AND p.nameZh IS NOT NULL AND p.nameZh LIKE CONCAT(:keyword, '%') THEN 5
          WHEN :keyword <> '' AND (
            UPPER(p.nameEn) LIKE UPPER(CONCAT('% ', :keyword, '%'))
            OR UPPER(p.nameEn) LIKE UPPER(CONCAT('%/', :keyword, '%'))
            OR UPPER(p.nameEn) LIKE UPPER(CONCAT('%-', :keyword, '%'))
            OR UPPER(p.nameEn) LIKE UPPER(CONCAT('%,', :keyword, '%'))
          ) THEN 6
          WHEN :keyword <> '' AND UPPER(p.code) LIKE UPPER(CONCAT('%', :keyword, '%')) THEN 7
          WHEN :keyword <> '' AND UPPER(p.nameEn) LIKE UPPER(CONCAT('%', :keyword, '%')) THEN 8
          WHEN p.nameZh IS NOT NULL AND TRIM(p.nameZh) <> '' THEN 9
          ELSE 10
        END,
        CASE p.portType
          WHEN com.furuiduo.quote.masterdata.entity.PortType.SEAPORT THEN 0
          WHEN com.furuiduo.quote.masterdata.entity.PortType.RAIL THEN 1
          WHEN com.furuiduo.quote.masterdata.entity.PortType.INLAND THEN 2
          ELSE 3
        END,
        p.nameEn ASC
      """)
  Page<MdGlobalPort> searchOptions(
      @Param("keyword") String keyword,
      @Param("portTypes") Collection<PortType> portTypes,
      @Param("portTypesEmpty") boolean portTypesEmpty,
      Pageable pageable);
}
