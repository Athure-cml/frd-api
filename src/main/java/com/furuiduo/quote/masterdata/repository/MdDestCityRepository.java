package com.furuiduo.quote.masterdata.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.furuiduo.quote.masterdata.entity.MdDestCity;

public interface MdDestCityRepository extends JpaRepository<MdDestCity, Long> {

  @Query(
      """
      SELECT c FROM MdDestCity c
      WHERE c.stateId = :stateId
        AND (
          :keyword = ''
          OR UPPER(c.name) LIKE UPPER(CONCAT('%', :keyword, '%'))
          OR EXISTS (
            SELECT 1 FROM MdDestZip z
            WHERE z.cityId = c.id
              AND UPPER(z.zipCode) LIKE UPPER(CONCAT('%', :keyword, '%'))
          )
        )
      ORDER BY c.name ASC
      """)
  List<MdDestCity> findByStateIdWithKeyword(
      @Param("stateId") Long stateId, @Param("keyword") String keyword);

  List<MdDestCity> findByStateIdOrderByNameAsc(Long stateId);

  List<MdDestCity> findAllByOrderByStateIdAscNameAsc();

  Optional<MdDestCity> findByStateIdAndNameIgnoreCase(Long stateId, String name);

  boolean existsByStateId(Long stateId);

  @Query(
      """
      SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
      FROM MdDestCity c
      WHERE UPPER(TRIM(c.name)) = UPPER(TRIM(:name))
      """)
  boolean existsByNameIgnoreCase(@Param("name") String name);

  @Query(
      """
      SELECT c.name FROM MdDestCity c
      WHERE (
          :keyword = ''
          OR UPPER(c.name) LIKE UPPER(CONCAT('%', :keyword, '%'))
        )
      GROUP BY c.name
      ORDER BY
        CASE
          WHEN :keyword <> '' AND UPPER(c.name) = UPPER(:keyword) THEN 0
          WHEN :keyword <> '' AND UPPER(c.name) LIKE UPPER(CONCAT(:keyword, '%')) THEN 1
          WHEN :keyword <> '' AND UPPER(c.name) LIKE UPPER(CONCAT('% ', :keyword, '%')) THEN 2
          ELSE 3
        END,
        c.name ASC
      """)
  org.springframework.data.domain.Page<String> searchDistinctNames(
      @Param("keyword") String keyword, org.springframework.data.domain.Pageable pageable);

  @Query(
      """
      SELECT s.code, LOWER(c.name), c.id
      FROM MdDestCity c, MdUsState s
      WHERE s.id = c.stateId
      """)
  List<Object[]> findExistingCityKeys();
}
