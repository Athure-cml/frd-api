package com.furuiduo.quote.masterdata.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.furuiduo.quote.masterdata.entity.MdInlandPor;

public interface MdInlandPorRepository extends JpaRepository<MdInlandPor, Long> {

  @Query(
      """
      SELECT p FROM MdInlandPor p
      WHERE (:name IS NULL OR UPPER(p.name) LIKE UPPER(CONCAT('%', :name, '%')))
        AND (:region IS NULL OR UPPER(p.region) LIKE UPPER(CONCAT('%', :region, '%')))
        AND (:polId IS NULL OR p.polId = :polId)
      ORDER BY p.name ASC
      """)
  List<MdInlandPor> search(
      @Param("name") String name, @Param("region") String region, @Param("polId") Long polId);

  boolean existsByPolId(Long polId);
}
