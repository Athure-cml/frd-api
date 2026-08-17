package com.furuiduo.quote.agent.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.furuiduo.quote.agent.entity.Agent;

public interface AgentRepository extends JpaRepository<Agent, Long> {

  boolean existsByCode(String code);

  Optional<Agent> findByCode(String code);

  @Query(
      """
      SELECT a FROM Agent a
      WHERE LOWER(TRIM(a.name)) = LOWER(TRIM(:name))
      """)
  Optional<Agent> findByNameNormalized(@Param("name") String name);

  @Query(
      """
      SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Agent a
      WHERE LOWER(TRIM(a.name)) = LOWER(TRIM(:name))
      AND (:excludeId IS NULL OR a.id <> :excludeId)
      """)
  boolean existsByNameNormalized(
      @Param("name") String name, @Param("excludeId") Long excludeId);

  @Query(
      """
      SELECT a FROM Agent a WHERE
      (:code = '' OR UPPER(a.code) LIKE UPPER(CONCAT('%', :code, '%')))
      AND (
        :name = ''
        OR UPPER(a.name) LIKE UPPER(CONCAT('%', :name, '%'))
        OR (a.shortName IS NOT NULL AND UPPER(a.shortName) LIKE UPPER(CONCAT('%', :name, '%')))
      )
      AND (:status IS NULL OR a.status = :status)
      """)
  Page<Agent> search(
      @Param("code") String code,
      @Param("name") String name,
      @Param("status") Integer status,
      Pageable pageable);

  @Query("SELECT a.code FROM Agent a WHERE a.code LIKE :prefix ORDER BY a.code DESC")
  Page<String> findCodesByPrefix(@Param("prefix") String prefix, Pageable pageable);

  Optional<Agent> findFirstByNameIgnoreCase(String name);

  @Query(
      """
      SELECT a FROM Agent a
      WHERE a.shortName IS NOT NULL
      AND TRIM(a.shortName) <> ''
      AND LOWER(TRIM(a.shortName)) = LOWER(TRIM(:shortName))
      """)
  Optional<Agent> findFirstByShortNameNormalized(@Param("shortName") String shortName);

  default Optional<Agent> findByNameOrShortName(String key) {
    if (key == null || key.isBlank()) {
      return Optional.empty();
    }
    String trimmed = key.trim();
    return findByNameNormalized(trimmed).or(() -> findFirstByShortNameNormalized(trimmed));
  }

  @Query(
      "SELECT a FROM Agent a WHERE (:pinned = true AND a.pinnedAt IS NOT NULL) OR (:pinned = false AND a.pinnedAt IS NULL) ORDER BY a.sortOrder ASC, a.updatedAt DESC")
  List<Agent> findAllByPinned(@Param("pinned") boolean pinned);

  @Query("SELECT COALESCE(MIN(a.sortOrder), 0) FROM Agent a WHERE a.pinnedAt IS NOT NULL")
  Integer minPinnedSortOrder();

  @Query("SELECT COALESCE(MAX(a.sortOrder), 0) FROM Agent a WHERE a.pinnedAt IS NULL")
  Integer maxUnpinnedSortOrder();
}
