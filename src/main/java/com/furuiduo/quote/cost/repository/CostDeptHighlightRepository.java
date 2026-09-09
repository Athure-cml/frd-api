package com.furuiduo.quote.cost.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.furuiduo.quote.cost.entity.CostDeptHighlight;
import com.furuiduo.quote.cost.entity.CostHighlightMode;

public interface CostDeptHighlightRepository extends JpaRepository<CostDeptHighlight, Long> {

  @Query(
      """
      SELECT h FROM CostDeptHighlight h
      JOIN FETCH h.department
      WHERE h.costMode = :mode AND h.costId IN :costIds
      """)
  List<CostDeptHighlight> findByCostModeAndCostIdIn(
      @Param("mode") CostHighlightMode costMode, @Param("costIds") Collection<Long> costIds);

  Optional<CostDeptHighlight> findByCostModeAndDepartmentIdAndCostId(
      CostHighlightMode costMode, Long departmentId, Long costId);

  @Query(
      """
      SELECT h.costId FROM CostDeptHighlight h
      WHERE h.costMode = :mode
        AND h.department.id IN :deptIds
      """)
  List<Long> findCostIdsByModeAndDeptIds(
      @Param("mode") CostHighlightMode mode, @Param("deptIds") Collection<Long> deptIds);

  @Query(
      """
      SELECT DISTINCT h.costId FROM CostDeptHighlight h
      WHERE h.costMode = :mode
        AND (h.department.id = :deptId OR h.adminShared = true)
      """)
  List<Long> findVisibleCostIdsForBusinessUser(
      @Param("mode") CostHighlightMode mode, @Param("deptId") Long deptId);

  void deleteByCostModeAndDepartmentIdAndCostIdIn(
      CostHighlightMode costMode, Long departmentId, Collection<Long> costIds);

  @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
  @Query(
      """
      DELETE FROM CostDeptHighlight h
      WHERE h.costMode = :mode AND h.costId IN :costIds
      """)
  int deleteAllByModeAndCostIds(
      @Param("mode") CostHighlightMode mode, @Param("costIds") Collection<Long> costIds);
}
