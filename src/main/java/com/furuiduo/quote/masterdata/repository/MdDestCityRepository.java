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
          :keyword IS NULL OR TRIM(:keyword) = ''
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
}
