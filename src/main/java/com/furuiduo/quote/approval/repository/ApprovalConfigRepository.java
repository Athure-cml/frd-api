package com.furuiduo.quote.approval.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.furuiduo.quote.approval.entity.ApprovalConfig;

public interface ApprovalConfigRepository extends JpaRepository<ApprovalConfig, Long> {

  @EntityGraph(attributePaths = "steps")
  @Query(
      """
      select c from ApprovalConfig c
      where (:configNo = '' or upper(c.configNo) like upper(concat('%', :configNo, '%')))
        and (:configObject = '' or c.configObject = :configObject)
      """)
  Page<ApprovalConfig> search(
      @Param("configNo") String configNo,
      @Param("configObject") String configObject,
      Pageable pageable);

  @EntityGraph(attributePaths = "steps")
  @Query("select c from ApprovalConfig c where c.id = :id")
  java.util.Optional<ApprovalConfig> findWithStepsById(@Param("id") Long id);

  @EntityGraph(attributePaths = "steps")
  java.util.List<ApprovalConfig> findByConfigObjectOrderByUpdatedAtDescIdDesc(
      String configObject);
}
