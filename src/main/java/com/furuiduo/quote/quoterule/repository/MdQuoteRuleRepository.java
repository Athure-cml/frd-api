package com.furuiduo.quote.quoterule.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.furuiduo.quote.quoterule.entity.MdQuoteRule;

public interface MdQuoteRuleRepository extends JpaRepository<MdQuoteRule, Long> {

  List<MdQuoteRule> findByStatusOrderBySortOrderAscIdAsc(Integer status);

  @Query(
      """
      SELECT r FROM MdQuoteRule r
      WHERE (:name = '' OR UPPER(r.name) LIKE UPPER(CONCAT('%', :name, '%')))
        AND (:targetField = '' OR r.targetField = :targetField)
        AND (:status IS NULL OR r.status = :status)
      ORDER BY r.sortOrder ASC, r.id ASC
      """)
  List<MdQuoteRule> search(
      @Param("name") String name,
      @Param("targetField") String targetField,
      @Param("status") Integer status);
}
